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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ratpack.exec.Promise
import ratpack.func.Block
import ratpack.http.MediaType
import ratpack.registry.Registry
import ratpack.render.Renderer
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch

class HealthCheckHandlerSpec extends RatpackGroovyDslSpec {

  static class HealthCheckFooHealthy implements HealthCheck {
    String getName() { return "foo" }

    Promise<HealthCheck.Result> check(Registry registry) throws Exception {
      return Promise.async { f ->
        f.success(HealthCheck.Result.healthy())
      }
    }
  }

  static class HealthCheckBarHealthy implements HealthCheck {
    String getName() { return "bar" }

    Promise<HealthCheck.Result> check(Registry registry) throws Exception {
      return Promise.async { f ->
        f.success(HealthCheck.Result.healthy())
      }
    }
  }

  static class HealthCheckFooUnhealthy implements HealthCheck {
    String getName() { return "foo" }

    Promise<HealthCheck.Result> check(Registry registry) throws Exception {
      return Promise.async { f ->
        f.success(HealthCheck.Result.unhealthy("EXECUTION TIMEOUT"))
      }
    }
  }

  static class HealthCheckFooUnhealthy2 implements HealthCheck {
    String getName() { return "foo" }

    Promise<HealthCheck.Result> check(Registry registry) throws Exception {
      throw new Exception("EXCEPTION PROMISE CREATION")
    }
  }

  static class HealthCheckParallel implements HealthCheck {
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

    Promise<HealthCheck.Result> check(Registry registry) throws Exception {
      return Promise.async { f ->
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

  def "render healthy check"() {
    when:
    bindings {
      bind HealthCheckFooHealthy
    }
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = get("health-checks")
    result.body.text == """foo : HEALTHY"""
    result.statusCode == 200
  }

  def "render healthy check with message"() {
    when:
    bindings {
      bindInstance HealthCheck, new HealthCheck() {
        @Override
        String getName() {
          return "foo"
        }

        @Override
        Promise<HealthCheck.Result> check(Registry registry) throws Exception {
          return Promise.value(HealthCheck.Result.healthy("some message"))
        }
      }
    }
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = get("health-checks")
    result.body.text == """foo : HEALTHY [some message]"""
    result.statusCode == 200
  }

  def "render unhealthy check"() {
    when:
    bindings {
      bind HealthCheckFooUnhealthy
    }
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = get("health-checks")
    result.body.text.startsWith("foo")
    result.body.text.contains("UNHEALTHY")
    result.body.text.contains("EXECUTION TIMEOUT")
    result.statusCode == 503
  }

  def "render unhealthy check with exception stack trace"() {
    when:
    bindings {
      bindInstance(HealthCheck, HealthCheck.of("bar") {
        Promise.async { f ->
          f.success(HealthCheck.Result.unhealthy("Custom exception message", new RuntimeException("Exception message")))
        }
      })
    }
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = getText("health-checks")
    assert result.startsWith("bar")
    assert result.contains("UNHEALTHY")
    assert result.contains("Custom exception message")
    assert result.contains("Exception message")
  }

  def "render unhealthy check with formatted message"() {
    when:
    bindings {
      bindInstance(HealthCheck, HealthCheck.of("bar") {
        Promise.async { f ->
          f.success(HealthCheck.Result.unhealthy("First value is: %s, second value is: %s", "eggs", "ham"))
        }
      })
    }
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = getText("health-checks")
    assert result.startsWith("bar")
    assert result.contains("UNHEALTHY")
    assert result.contains("First value is: eggs, second value is: ham")
  }


  def "render unhealthy check while promise itself throwing exception"() {
    when:
    bindings {
      bindInstance(HealthCheck, HealthCheck.of("bar") {
        Promise.async { f ->
          throw new Exception("EXCEPTION FROM PROMISE")
        }
      })
    }
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = getText("health-checks")
    assert result.startsWith("bar")
    assert result.contains("UNHEALTHY")
    assert result.contains("EXCEPTION FROM PROMISE")
    assert result.contains("Exception")
  }

  def "render unhealthy check while promise creation throwing exception"() {
    when:
    bindings {
      bind HealthCheckFooUnhealthy2
    }
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = getText("health-checks")
    assert result.startsWith("foo")
    assert result.contains("UNHEALTHY")
    assert result.contains("EXCEPTION PROMISE CREATION")
  }

  def "render nothing if no health check in registry"() {
    when:
    handlers {
      register {
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    with(get("health-checks")) {
      body.text.empty
      statusCode == 200
    }
  }

  def "render healthy check results for more health checks"() {
    when:
    bindings {
      bind HealthCheckFooHealthy
      bind HealthCheckBarHealthy
    }
    handlers {
      register {
        add HealthCheck.of("baz") {
          Promise.async { f ->
            f.success(HealthCheck.Result.healthy())
          }
        }
        add HealthCheck.of("quux") {
          Promise.async { f ->
            f.success(HealthCheck.Result.healthy())
          }
        }
      }
      get("health-checks", new HealthCheckHandler())
      get("health-checks/:name") { ctx ->
        new HealthCheckHandler(pathTokens["name"]).handle(ctx)
      }
    }

    then:
    def result = getText("health-checks")
    def results = result.split("\n")
    results.length == 4
    results[0].startsWith("bar")
    results[0].contains("HEALTHY")
    results[1].startsWith("baz")
    results[1].contains("HEALTHY")
    results[2].startsWith("foo")
    results[2].contains("HEALTHY")
    results[3].startsWith("quux")
    results[3].contains("HEALTHY")
  }

  def "health checks run in parallel"() {
    given:
    CountDownLatch latch = new CountDownLatch(1)

    when:
    handlers {
      register {
        add HealthCheck.of("baz") {
          Promise.async { f ->
            latch.await()
            f.success(HealthCheck.Result.healthy())
          }
        }
        add HealthCheck.of("quux") {
          Promise.async { f ->
            latch.countDown()
            f.success(HealthCheck.Result.healthy())
          }
        }
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = getText("health-checks")
    String[] results = result.split("\n")
    results[0].startsWith("baz")
    results[0].contains("HEALTHY")
    results[1].startsWith("quux")
    results[1].contains("HEALTHY")
  }

  def "duplicated health checks renders only once"() {
    when:
    bindings {
      bind HealthCheckFooHealthy
      bind HealthCheckFooHealthy
    }
    handlers {
      register {
        add HealthCheck.of("foo") {
          Promise.async { f ->
            f.success(HealthCheck.Result.unhealthy("Unhealthy"))
          }
        }
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = getText("health-checks")
    String[] results = result.split("\n")
    results.size() == 1
    results[0].startsWith("foo")
    results[0].contains("HEALTHY")
  }

  def "render health check by token if more health checks in registry"() {
    when:
    bindings {
      bind HealthCheckFooHealthy
      bind HealthCheckBarHealthy
    }
    handlers {
      register {
        add HealthCheck.of("baz") { ec ->
          Promise.async { f ->
            f.success(HealthCheck.Result.unhealthy("Unhealthy"))
          }
        }
        add HealthCheck.of("quux") { ec ->
          Promise.async { f ->
            f.success(HealthCheck.Result.healthy())
          }
        }
      }
      get("health-checks/:name", new HealthCheckHandler())
    }

    then:
    def result = getText("health-checks/foo")
    String[] results = result.split("\n")
    results.length == 1
    results[0].startsWith("foo")
    results[0].contains("HEALTHY")

    then:
    def result2 = getText("health-checks/baz")
    String[] results2 = result2.split("\n")
    results.length == 1
    results2[0].startsWith("baz")
    results2[0].contains("UNHEALTHY")
  }

  def "render json health check results for custom renderer"() {
    given:
    def json = new JsonSlurper()

    when:
    bindings {
      bind HealthCheckFooHealthy
      bind HealthCheckBarHealthy
    }
    handlers {
      register {
        add(Renderer.of(HealthCheckResults) { ctx, r ->
          ctx.byContent {
            it.json({ ->
              ctx.render(JsonOutput.toJson(r.results))
            } as Block)
          }
        })
        add HealthCheck.of("baz") {
          Promise.async { f ->
            f.success(HealthCheck.Result.unhealthy("Unhealthy"))
          }
        }
      }
      get("health-checks", new HealthCheckHandler())
    }

    then:
    requestSpec { spec ->
      spec.headers.add("Accept", "application/json")
    }
    def result = get("health-checks")
    result.body.contentType.toString() == MediaType.APPLICATION_JSON
    Map<String, Map<String, ?>> results = json.parse(result.body.inputStream)
    results.foo.healthy == true
    results.bar.healthy == true
    results.baz.healthy == false
    results.baz.message == "Unhealthy"
  }

}
