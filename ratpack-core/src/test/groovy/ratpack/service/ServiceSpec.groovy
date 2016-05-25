/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.service

import ratpack.exec.Operation
import ratpack.func.Predicate
import ratpack.server.StartupFailureException
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CyclicBarrier

class ServiceSpec extends RatpackGroovyDslSpec {

  List<String> events = [].asSynchronized()

  class RecordingService implements Service {

    String prefix = ""
    Runnable onStart
    Runnable onStop

    @Override
    void onStart(StartEvent event) throws Exception {
      events << "${prefix ? prefix + "-" : ""}start".toString()
      if (onStart) {
        Operation.of { onStart.run() }.then()
      }
    }

    @Override
    void onStop(StopEvent event) throws Exception {
      events << "${prefix ? prefix + "-" : ""}stop".toString()
      if (onStop) {
        Operation.of { onStop.run() }.then()
      }
    }
  }

  def "services can be async"() {
    when:
    bindings {
      bindInstance new RecordingService()
    }
    handlers {
      get {
        render events.toString()
      }
    }

    then:
    text == ["start"].toString()

    when:
    server.stop()

    then:
    events == ["start", "stop"]
  }

  def "services initialised in dependency order"() {
    when:
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new RecordingService(prefix: "1")
      multiBindInstance new RecordingService(prefix: "2")
      multiBindInstance new RecordingService(prefix: "3")
      multiBindInstance dependsOn("2", "1")
      multiBindInstance dependsOn("3", "2")
    }
    handlers {
      get {
        render events.toString()
      }
    }

    then:
    text == ["1-start", "2-start", "3-start"].toString()

    when:
    server.stop()

    then:
    events == ["1-start", "2-start", "3-start", "3-stop", "2-stop", "1-stop"]
  }

  def "all dependencies are satisfied before starting a service"() {
    when:
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new RecordingService(prefix: "1")
      multiBindInstance new RecordingService(prefix: "2")
      multiBindInstance new RecordingService(prefix: "3")
      multiBindInstance dependsOn("3", "2")
      multiBindInstance dependsOn("3", "1")
    }
    handlers {
      get {
        render events.toString()
      }
    }

    then:
    get()
    events[0] in ["2-start", "1-start"]
    events[1] in ["2-start", "1-start"]
    events[2] in ["3-start"]

    when:
    server.stop()

    then:
    events[3] == "3-stop"
    events[4] in ["2-stop", "1-stop"]
    events[5] in ["2-stop", "1-stop"]
  }

  def "startup stops when the first service errors"() {
    when:
    serverConfig { development(false) }
    bindings {
      multiBindInstance new RecordingService(prefix: "1")
      multiBindInstance new RecordingService(prefix: "2", onStart: { throw new Exception("!") })
      multiBindInstance new RecordingService(prefix: "3")
      multiBindInstance dependsOn("2", "1")
      multiBindInstance dependsOn("3", "2")
    }
    server.start()

    then:
    def e = thrown StartupFailureException
    e.cause.message == "!"
    events == ["1-start", "2-start", "1-stop"]
  }

  def "services are initialised in parallel"() {
    when:
    def barrier = new CyclicBarrier(3)
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new RecordingService(onStart: { barrier.await() }, onStop: { barrier.await() })
      multiBindInstance new RecordingService(onStart: { barrier.await() }, onStop: { barrier.await() })
      multiBindInstance new RecordingService(onStart: { barrier.await() }, onStop: { barrier.await() })
    }

    and:
    server.start()
    server.stop()

    then:
    events.size() == 6
  }

  def "detects cycles where none can start"() {
    when:
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new RecordingService(prefix: "1")
      multiBindInstance new RecordingService(prefix: "2")
      multiBindInstance dependsOn("1", "2")
      multiBindInstance dependsOn("2", "1")
    }
    server.start()

    then:
    thrown StartupFailureException
  }

  def "detects cycles where some can start"() {
    when:
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new RecordingService(prefix: "1")
      multiBindInstance new RecordingService(prefix: "2")
      multiBindInstance new RecordingService(prefix: "3")
      multiBindInstance dependsOn("1", "2")
      multiBindInstance dependsOn("2", "1")
    }
    server.start()

    then:
    events == ["3-start", "3-stop"]
    thrown StartupFailureException
  }

  class S1 extends RecordingService {
    S1() {
      prefix = "1"
    }
  }

  @DependsOn(S1)
  class S2 extends RecordingService {
    S2() {
      prefix = "2"
    }
  }

  @DependsOn(S2)
  class S3 extends RecordingService {
    S3() {
      prefix = "3"
    }
  }

  def "can declare dependencies via annotation"() {
    when:
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new S1()
      multiBindInstance new S2()
      multiBindInstance new S3()
    }
    handlers {
      get {
        render events.toString()
      }
    }

    then:
    text == ["1-start", "2-start", "3-start"].toString()

    when:
    server.stop()

    then:
    events == ["1-start", "2-start", "3-start", "3-stop", "2-stop", "1-stop"]
  }

  def "can create services from actions"() {
    when:
    bindings {
      multiBindInstance Service.startup("1") {
        events << "start".toString()
      }
      multiBindInstance Service.shutdown("2") {
        events << "stop".toString()
      }
    }
    handlers {
      get {
        render events.toString()
      }
    }

    then:
    text == ["start"].toString()

    when:
    server.stop()

    then:
    events == ["start", "stop"]
  }

  static ServiceDependencies dependsOn(String dependentPrefix, String dependencyPrefix) {
    dependsOn(
      { it instanceof RecordingService && it.prefix == dependentPrefix },
      { it instanceof RecordingService && it.prefix == dependencyPrefix }
    )
  }

  static ServiceDependencies dependsOn(Predicate<?> dependents, Predicate<?> dependencies) {
    return { it.dependsOn(dependents, dependencies) }
  }
}

