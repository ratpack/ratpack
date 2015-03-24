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

package ratpack.health

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import ratpack.exec.ExecControl
import ratpack.exec.ExecController
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.MediaType
import ratpack.render.Renderer
import ratpack.test.embed.EmbeddedApp
import ratpack.test.http.TestHttpClient
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

class HealthCheckFooHealthy implements HealthCheck {
  String getName() { return "foo" }

  Promise<HealthCheck.Result> check(ExecControl execControl) throws Exception {
    return execControl.promise { f ->
      f.success(HealthCheck.Result.healthy())
    }
  }
}

class HealthCheckBarHealthy implements HealthCheck {
  String getName() { return "bar" }

  Promise<HealthCheck.Result> check(ExecControl execControl) throws Exception {
    return execControl.promise { f ->
      f.success(HealthCheck.Result.healthy())
    }
  }
}


class HealthCheckFooUnhealthy implements HealthCheck {
  String getName() { return "foo" }

  Promise<HealthCheck.Result> check(ExecControl execControl) throws Exception {
    return execControl.promise { f ->
      f.success(HealthCheck.Result.unhealthy("EXECUTION TIMEOUT"))
    }
  }
}

class HealthCheckFooUnhealthy2 implements HealthCheck {
  String getName() { return "foo"}

  Promise<HealthCheck.Result> check(ExecControl execControl) throws Exception {
    throw new Exception("EXCEPTION PROMISE CREATION")
  }
}

class HealthCheckParallel implements HealthCheck {
  private final String name
  private CountDownLatch waitingFor
  private CountDownLatch finalized
  private List<String> output

  HealthCheckParallel(String name, CountDownLatch waitingFor, CountDownLatch finalized, List<String> output) {
    this.name = name
    this.waitingFor = waitingFor
    this.finalized = finalized
    this.output = output
  }

  String getName() { return this.name }

  Promise<HealthCheck.Result> check(ExecControl execControl) throws Exception {
    return execControl.promise { f ->
      if (waitingFor) {
        waitingFor.await()
      }
      output << name
      f.success(HealthCheck.Result.healthy())
      if (finalized) {
        finalized.countDown()
      }
    }
  }
}

class HealthCheckHandlerSpec extends Specification {
  def "render healthy check"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bind HealthCheckFooHealthy
      }
      handlers {
        register {
          add new HealthCheckResultsRenderer()
        }
        get("health-checks", new HealthCheckHandler())
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks")
      assert result.startsWith("foo")
      assert result.contains("HEALTHY")
    }
  }

  def "render unhealthy check"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bind HealthCheckFooUnhealthy
      }
      handlers {
        register {
          add new HealthCheckResultsRenderer()
        }
        get("health-checks", new HealthCheckHandler())
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks")
      assert result.startsWith("foo")
      assert result.contains("UNHEALTHY")
      assert result.contains("EXECUTION TIMEOUT")
    }
  }

  def "render unhealthy check while promise itself throwning exception"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bindInstance(HealthCheck, HealthCheck.of("bar") { execControl ->
          execControl.promise { f ->
            throw new Exception("EXCEPTION FROM PROMISE")
          }
        })
      }
      handlers {
        register {
          add new HealthCheckResultsRenderer()
        }
        get("health-checks", new HealthCheckHandler())
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks")
      assert result.startsWith("bar")
      assert result.contains("UNHEALTHY")
      assert result.contains("EXCEPTION FROM PROMISE")
      assert result.contains("Exception")
    }
  }

  def "render unhealthy check while promise creation throwning exception"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bind HealthCheckFooUnhealthy2
      }
      handlers {
        register {
          add new HealthCheckResultsRenderer()
        }
        get("health-checks", new HealthCheckHandler())
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks")
      assert result.startsWith("foo")
      assert result.contains("UNHEALTHY")
      assert result.contains("EXCEPTION PROMISE CREATION")
    }
  }

  def "render nothing if no health check in registry"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        register {
          add new HealthCheckResultsRenderer()
        }
        get("health-checks", new HealthCheckHandler())
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      assert httpClient.getText("health-checks").isEmpty()
    }
  }

  def "render healthy check results for more health checks"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bind HealthCheckFooHealthy
        bind HealthCheckBarHealthy
      }
      handlers {
        register {
          add new HealthCheckResultsRenderer()
          add HealthCheck.of("baz") { ec ->
            ec.promise { f ->
              f.success(HealthCheck.Result.healthy())
            }
          }
          add HealthCheck.of("quux") { ec ->
            ec.promise { f ->
              f.success(HealthCheck.Result.healthy())
            }
          }
        }
        get("health-checks", new HealthCheckHandler())
        get("health-checks/:name") { ctx ->
          new HealthCheckHandler(pathTokens["name"]).handle(ctx)
        }
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks")
      //assert result == "zzz"
      String[] results = result.split("\n")
      assert results.length == 4
      assert results[0].startsWith("bar")
      assert results[0].contains("HEALTHY")
      assert results[1].startsWith("baz")
      assert results[1].contains("HEALTHY")
      assert results[2].startsWith("foo")
      assert results[2].contains("HEALTHY")
      assert results[3].startsWith("quux")
      assert results[3].contains("HEALTHY")
    }
  }

  def "health checks run in parallel"() {
    given:
    CountDownLatch latch = new CountDownLatch(1)

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        register {
          add new HealthCheckResultsRenderer()
          add HealthCheck.of("baz") { ec ->
            ec.promise { f ->
              latch.await()
              f.success(HealthCheck.Result.healthy())
            }
          }
          add HealthCheck.of("quux") { ec ->
            ec.promise { f ->
              latch.countDown()
              f.success(HealthCheck.Result.healthy())
            }
          }
        }
        get("health-checks", new HealthCheckHandler())
      }
    }
    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks")
      String[] results = result.split("\n")
      assert results[0].startsWith("baz")
      assert results[0].contains("HEALTHY")
      assert results[1].startsWith("quux")
      assert results[1].contains("HEALTHY")
    }
  }

  def "duplicated health checks renders only once"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bind HealthCheckFooHealthy
        bind HealthCheckFooHealthy
      }
      handlers {
        register {
          add new HealthCheckResultsRenderer()
          add HealthCheck.of("foo") { ec ->
            ec.promise { f ->
              latch.await()
              f.success(HealthCheck.Result.unhealthy("Unhealthy"))
            }
          }
        }
        get("health-checks", new HealthCheckHandler())
      }
    }
    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks")
      String[] results = result.split("\n")
      assert results.size() == 1
      assert results[0].startsWith("foo")
      assert results[0].contains("HEALTHY")
    }
  }

  def "render health check by token if more health checks in registry"() {
    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bind HealthCheckFooHealthy
        bind HealthCheckBarHealthy
      }
      handlers {
        register {
          add new HealthCheckResultsRenderer()
          add HealthCheck.of("baz") { ec ->
            ec.promise { f ->
              f.success(HealthCheck.Result.unhealthy("Unhealthy"))
            }
          }
          add HealthCheck.of("quux") { ec ->
            ec.promise { f ->
              f.success(HealthCheck.Result.healthy())
            }
          }
        }
        get("health-checks/:name") { ctx ->
          new HealthCheckHandler(pathTokens["name"]).handle(ctx)
        }
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      def result = httpClient.getText("health-checks/foo")
      String[] results = result.split("\n")
      assert results.length == 1
      assert results[0].startsWith("foo")
      assert results[0].contains("HEALTHY")

      result = httpClient.getText("health-checks/baz")
      results = result.split("\n")
      assert results[0].startsWith("baz")
      assert results[0].contains("UNHEALTHY")
    }
  }

  def "render json health check results for custom renderer"() {
    given:
    def json = new JsonSlurper()

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      bindings {
        bind HealthCheckFooHealthy
        bind HealthCheckBarHealthy
      }
      handlers {
        register {
          add(Renderer.of(HealthCheckResults) { ctx, r ->
            def headers = ctx.request.headers
            if (headers?.get("Accept") == "application/json" || headers?.get("Content-Type") == "application/json") {
              ctx.response.headers
                .add("Cache-Control", "no-cache, no-store, must-revalidate")
                .add("Pragma", "no-cache")
                .add("Expires", 0)
                .add("Content-Type", "application/json")
              ctx.render(JsonOutput.toJson(r.getResults()))
            }
            else {
              // no caching headers are set inside default renderer
              new HealthCheckResultsRenderer().render(ctx, r)
            }
          })
          add HealthCheck.of("baz") { ec ->
            ec.promise { f ->
              f.success(HealthCheck.Result.unhealthy("Unhealthy"))
            }
          }
        }
        get("health-checks", new HealthCheckHandler())
      }
    }

    then:
    app.test{TestHttpClient httpClient ->
      httpClient.requestSpec{ spec ->
        spec.headers.add("Accept", "application/json")
      }
      def result = httpClient.get("health-checks")
      assert result.body.contentType.toString() == MediaType.APPLICATION_JSON
      def results = json.parse(result.body.inputStream)
      assert results.foo.healthy == true
      assert results.bar.healthy == true
      assert results.baz.healthy == false
      assert results.baz.message == "Unhealthy"
    }
  }

  def "handler with concurrencyLevel=0 run ordered (by name) health checks in parallel"() {
    given:
    def output = []
    def countDownLatches = []
    int eventLoopThreads = 0

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers { ExecController ec ->
        register {
          add new HealthCheckResultsRenderer()

          eventLoopThreads = ec.getNumThreads()
          for (int i=0; i<eventLoopThreads; i++) {
            countDownLatches.add(new CountDownLatch(1))

          }
          for (int i=0; i<eventLoopThreads; i++) {
            add new HealthCheckParallel("foo${i+1}", i == (eventLoopThreads-1) ? null : countDownLatches[i+1], countDownLatches[i], output)
          }
        }
        get("health-checks", new HealthCheckHandler(0))
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      httpClient.getText("health-checks")
      def expectedOutput = []
      for (int i=eventLoopThreads-1; i>=0; i--) {
        expectedOutput.add("foo${i+1}")
      }
      assert output == expectedOutput
    }
  }

  def "handler with concurrencyLevel=1 run ordered (by name) health checks in sequence"() {
    given:
    def output = []
    int numOfHealthChecks = 8

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        register {
          add new HealthCheckResultsRenderer()
          for (int i=0; i<numOfHealthChecks; i++) {
            add new HealthCheckParallel("foo${i+1}", null, null, output)
          }
        }
        get("health-checks", new HealthCheckHandler(1))
      }
    }

    then:
    app.test { TestHttpClient httpClient ->
      httpClient.getText("health-checks")
      def expectedOutput = []
      for (int i=0; i<numOfHealthChecks; i++) {
        expectedOutput.add("foo${i+1}")
      }
      assert output == expectedOutput
    }
  }

  def "handler with currencyLevel=2 run ordered (by name) health checks in sequence of 2 parallel"() {
    given:
    def output = []
    int numOfHealthChecks = 8
    def countDownLatches = []
    for (int i=0; i<numOfHealthChecks; i++) {
      countDownLatches.add(new CountDownLatch(1))
    }

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        register {
          add new HealthCheckResultsRenderer()

          for (int i=0; i<numOfHealthChecks; i++) {
            // second in group is waiting for the first one
            // pairs run in parallel while groups of two in sequence
            add new HealthCheckParallel("foo${i+1}", i%2 ?  null : countDownLatches[i+1], countDownLatches[i], output)
          }
        }
        get("health-checks", new HealthCheckHandler(2))
      }
    }

    then:
    app.test{ TestHttpClient httpClient ->
      httpClient.getText("health-checks")
      def expectedOutput = ["foo2", "foo1", "foo4", "foo3", "foo6", "foo5", "foo8", "foo7"]
      assert output == expectedOutput
    }
  }

  def "handler with currencyLevel=3 run ordered (by name) health checks in sequence of 3 parallel"() {
    given:
    def output = []
    int numOfHealthChecks = 9
    def countDownLatches = []
    for (int i=0; i<numOfHealthChecks; i++) {
      countDownLatches.add(new CountDownLatch(1))
    }

    when:
    EmbeddedApp app = GroovyEmbeddedApp.build {
      handlers {
        register {
          add new HealthCheckResultsRenderer()

          int k = 0
          for (int i=0; i<numOfHealthChecks; i++) {
            // third in group is waiting for second and second is waiting for the first one
            // triples run in parallel while groups of three in sequence

            add new HealthCheckParallel("foo${i+1}", k in [0,1] ? countDownLatches[i+1] : null , countDownLatches[i], output)
            if (++k == 3) {
              k = 0
            }
          }
        }
        get("health-checks", new HealthCheckHandler(3))
      }
    }

    then:
    app.test{ TestHttpClient httpClient ->
      httpClient.getText("health-checks")
      def expectedOutput = ["foo3", "foo2", "foo1", "foo6", "foo5", "foo4", "foo9", "foo8", "foo7"]
      assert output == expectedOutput
    }
  }
}
