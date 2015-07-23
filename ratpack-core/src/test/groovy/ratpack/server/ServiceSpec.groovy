/*
 * Copyright 2015 the original author or authors.
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

package ratpack.server

import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec

class ServiceSpec extends RatpackGroovyDslSpec {

  def events = []

  class RecordingService implements Service {

    String prefix = ""

    @Override
    void onStart(StartEvent event) throws Exception {
      Blocking.get { "${prefix}start".toString() }.then { events << it }
    }

    @Override
    void onStop(StopEvent event) throws Exception {
      Blocking.get { "${prefix}stop".toString() }.then { events << it }
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

  def "services are executed in order returned by the registry"() {
    when:
    bindings {
      multiBindInstance new RecordingService(prefix: "2 ")
      multiBindInstance new RecordingService(prefix: "1 ")
    }
    handlers {
      get {
        render events.toString()
      }
    }

    then:
    text == ["1 start", "2 start"].toString()

    when:
    server.stop()

    then:
    events == ["1 start", "2 start", "2 stop", "1 stop"]
  }

  def "startup stops when the first service errors"() {
    when:
    serverConfig { development(false) }
    bindings {
      multiBindInstance new RecordingService(prefix: "2 ")
      bindInstance(new Service() {
        @Override
        void onStart(StartEvent event) throws Exception {
          throw new Exception("!")
        }
      })
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
    events == ["2 stop"] // no other services started
  }

  def "startup stops when the first service errors async"() {
    when:
    serverConfig { development(false) }
    bindings {
      multiBindInstance new RecordingService(prefix: "2 ")
      bindInstance(new Service() {
        @Override
        void onStart(StartEvent event) throws Exception {
          Blocking.get { throw new Exception("!") }.then {}
        }

        @Override
        void onStop(StopEvent event) throws Exception {
          events << "error-stop"
        }
      })
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
    events == ["2 stop", "error-stop"] // no other services started
  }

}
