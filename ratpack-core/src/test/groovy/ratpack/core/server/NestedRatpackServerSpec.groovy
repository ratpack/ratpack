/*
 * Copyright 2014 the original author or authors.
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

package ratpack.core.server

import ratpack.core.handling.Handler
import ratpack.core.http.client.HttpClient
import ratpack.core.service.Service
import ratpack.core.service.StartEvent
import ratpack.core.service.StopEvent
import ratpack.exec.Blocking
import ratpack.exec.ExecController
import ratpack.exec.ExecInitializer
import ratpack.exec.Execution
import ratpack.test.ServerBackedApplicationUnderTest
import ratpack.test.http.TestHttpClient
import ratpack.test.internal.BaseRatpackSpec
import spock.lang.AutoCleanup

class NestedRatpackServerSpec extends BaseRatpackSpec {

  @AutoCleanup("stop")
  RatpackServer server
  def http = TestHttpClient.testHttpClient(ServerBackedApplicationUnderTest.of { server })

  def events = [].asSynchronized() as List<String>

  def "can use nested server - useExplicit: #useExplicit"() {
    given:
    server = RatpackServer.of { spec ->
      spec
        .serverConfig { it.development(false).port(0) }
        .registryOf { r ->
          r.add(new EventService("outer"))
          r.add(new EventInitializer("outer"))
        }
        .handler { registry ->
          return { outerCtx ->
            def httpClient = registry.get(HttpClient)
            def outerExecController = ExecController.require()
            def innerServer = RatpackServer.of {
              it.serverConfig {
                it.with {
                  development(false)
                  port(0)
                  if (useExplicit) {
                    inheritExecController(true)
                  } else {
                    execController(outerExecController)
                  }
                }
              }
              it.registryOf {
                it.add(new EventService("inner"))
                it.add(new EventInitializer("inner"))
              }
              it.handler { innerRegistry ->
                { ctx ->
                  assert innerRegistry.get(ExecController).is(outerExecController)
                  assert ExecController.require().is(outerExecController)
                  ctx.render("inner")
                } as Handler
              }
            }

            Blocking.op { innerServer.start() }
              .flatMap {
                httpClient.get(URI.create("http://localhost:$innerServer.bindPort"))
                  .map { it.body.text }
              }
              .blockingOp { innerServer.stop() }
              .then { outerCtx.render(it) }
          } as Handler
        }
    }

    when:
    server.start()

    then:
    http.text == "inner"

    when:
    server.stop()

    then:
    def expectedEvents = [
      "outer-exec-init", // start outer service execution
      "outer-onStart",

      "outer-exec-init", // outer request

      "outer-exec-init", // start inner service execution
      "inner-onStart",

      "outer-exec-init", // inner request

      "outer-exec-init", // stop inner service execution
      "inner-onStop",

      "outer-exec-init", // stop outer service execution
      "outer-onStop"
    ].toListString()
    def actualEvents = events.toListString()
    actualEvents == expectedEvents

    where:
    useExplicit << [true, false]
  }

  class EventService implements Service {

    String name

    EventService(String name) {
      this.name = name
    }

    @Override
    void onStart(StartEvent event) throws Exception {
      events << "$name-onStart".toString()
    }

    @Override
    void onStop(StopEvent event) throws Exception {
      events << "$name-onStop".toString()
    }
  }

  class EventInitializer implements ExecInitializer {
    String name
    volatile int i

    EventInitializer(String name) {
      this.name = name
    }

    @Override
    void init(Execution execution) {
      ++i
      events << "$name-exec-init".toString()
    }
  }

}
