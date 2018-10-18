/*
 * Copyright 2018 the original author or authors.
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

package ratpack.hikari

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import ratpack.health.HealthCheckHandler
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Inject
import java.time.Duration

class HikariHealthCheckSpec extends RatpackGroovyDslSpec {

  def "be healthy when connection is open"() {
    when:
    bindings {
      module HikariModule, {
        it.addDataSourceProperty("URL", "jdbc:h2:mem:dev")
        it.dataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
      }

      bind QuuxHealthCheck
    }

    handlers {
      get("health-checks", new HealthCheckHandler())
    }

    then:
    def result = get("health-checks")
    result.body.text == """quux : HEALTHY"""
    result.statusCode == 200
  }

  def "be unhealthy when connection does not work"() {
    when:
    bindings {
      module HikariModule, {
        it.addDataSourceProperty("URL", "jdbc:h2:mem:dev")
        it.dataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
      }

      bind FubarHealthCheck
    }

    handlers {
      get("kill-hikari", { ctx ->
        ctx.get(HikariDataSource).close()
        ctx.response.send()
      })
      get("health-checks", new HealthCheckHandler())
    }

    then:
    get("kill-hikari").statusCode == 200
    def result = get("health-checks")
    result.body.text.startsWith("FUBAR : UNHEALTHY [Hikari couldn't get a connection after 0s]")
    result.statusCode == 503
  }

  static class QuuxHealthCheck extends HikariHealthCheck {

    HikariPool hikariPool
    String name = "quux"

    @Inject
    QuuxHealthCheck(HikariPool hikariPool) {
      this.hikariPool = hikariPool
    }
  }

  static class FubarHealthCheck extends HikariHealthCheck {

    HikariPool hikariPool
    String name = "FUBAR"
    Duration timeout = Duration.ofMillis(100)

    @Inject
    FubarHealthCheck(HikariPool hikariPool) {
      this.hikariPool = hikariPool
    }
  }
}
