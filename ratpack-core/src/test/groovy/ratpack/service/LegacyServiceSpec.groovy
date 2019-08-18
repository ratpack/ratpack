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

class LegacyServiceSpec extends RatpackGroovyDslSpec {

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

  @DependsOn(LegacyRecordingService)
  class DependentRecordingService extends RecordingService {

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

  @SuppressWarnings("GrDeprecatedAPIUsage")
  class LegacyRecordingService implements ratpack.server.Service {

    String prefix = ""
    Runnable onStart
    Runnable onStop

    @Override
    void onStart(ratpack.server.StartEvent event) throws Exception {
      events << "${prefix ? prefix + "-" : ""}start".toString()
      if (onStart) {
        Operation.of { onStart.run() }.then()
      }
    }

    @Override
    void onStop(ratpack.server.StopEvent event) throws Exception {
      events << "${prefix ? prefix + "-" : ""}stop".toString()
      if (onStop) {
        Operation.of { onStop.run() }.then()
      }
    }
  }

  def "services can be async"() {
    when:
    bindings {
      bindInstance new LegacyRecordingService()
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

  def "services initialised in retrieval order"() {
    when:
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new LegacyRecordingService(prefix: "3")
      multiBindInstance new LegacyRecordingService(prefix: "2")
      multiBindInstance new LegacyRecordingService(prefix: "1")
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

  def "startup stops when the first service errors"() {
    when:
    serverConfig { development(false) }
    bindings {
      multiBindInstance new LegacyRecordingService(prefix: "3")
      multiBindInstance new LegacyRecordingService(prefix: "2", onStart: { throw new Exception("!") })
      multiBindInstance new LegacyRecordingService(prefix: "1")
    }
    handlers {
      get {
        render events.toString()
      }
    }

    and:
    server.start()

    then:
    def e = thrown StartupFailureException
    e.cause.message == "!"
    events == ["1-start", "2-start", "1-stop"]
  }

  def "detects cycles where none can start"() {
    when:
    serverConfig {
      threads 3
    }
    bindings {
      multiBindInstance new LegacyRecordingService(prefix: "1")
      multiBindInstance new LegacyRecordingService(prefix: "2")
      multiBindInstance dependsOn("1", "2")
    }
    server.start()

    then:
    thrown StartupFailureException
  }

  def "can define dependencies between legacy and non"() {
    when:
    bindings {
      multiBindInstance new LegacyRecordingService(prefix: "2")
      multiBindInstance new LegacyRecordingService(prefix: "1")
      multiBindInstance new RecordingService(prefix: "3")
      multiBindInstance new RecordingService(prefix: "4")
      multiBindInstance dependsOn("3", "1")
      multiBindInstance dependsOn("3", "2")
      multiBindInstance dependsOn("2", "4")
      multiBindInstance dependsOn("4", "1")
    }
    server.start()
    server.stop()

    then:
    events == ["1-start", "4-start", "2-start", "3-start", "3-stop", "2-stop", "4-stop", "1-stop"]
  }

  def "can define dependencies between legacy and non using annotation"() {
    when:
    bindings {
      multiBindInstance new LegacyRecordingService(prefix: "1")
      multiBindInstance new DependentRecordingService(prefix: "2")
    }
    server.start()
    server.stop()

    then:
    events == ["1-start", "2-start", "2-stop", "1-stop"]
  }

  static ServiceDependencies dependsOn(String dependentPrefix, String dependencyPrefix) {
    dependsOn(
      { unpack(it).prefix == dependentPrefix },
      { unpack(it).prefix == dependencyPrefix }
    )
  }

  static unpack(Service service) {
    service instanceof LegacyServiceAdapter ? service.adapted : service
  }

  static ServiceDependencies dependsOn(Predicate<?> dependents, Predicate<?> dependencies) {
    return { it.dependsOn(dependents, dependencies) }
  }

}
