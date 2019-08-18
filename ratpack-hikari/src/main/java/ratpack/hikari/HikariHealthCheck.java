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

package ratpack.hikari;

import com.zaxxer.hikari.pool.HikariPool;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.health.HealthCheck;
import ratpack.registry.Registry;

import java.sql.SQLException;
import java.time.Duration;

/**
 * Reports on the health of HikariCP JDBC connection pool.
 *
 * <pre class="java">{@code
 * import com.zaxxer.hikari.pool.HikariPool;
 * import ratpack.guice.Guice;
 * import ratpack.health.HealthCheckHandler;
 * import ratpack.hikari.HikariHealthCheck;
 * import ratpack.hikari.HikariModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import javax.inject.Inject;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b -> b
 *         .module(HikariModule.class, hikariConfig -> {
 *           hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
 *           hikariConfig.addDataSourceProperty("URL", "jdbc:h2:mem:dev"); // Use H2 in memory database
 *         })
 *
 *         .bind(MyHealthCheck.class)
 *       ))
 *       .handlers(chain -> chain
 *         .get("health-checks", new HealthCheckHandler())
 *       )
 *     ).test(httpClient -> assertEquals("my-health-check : HEALTHY", httpClient.getText("health-checks")));
 *   }
 *
 *   static class MyHealthCheck extends HikariHealthCheck {
 *
 *     private HikariPool hikariPool;
 *
 *     {@literal @}Inject
 *     MyHealthCheck(HikariPool hikariPool) {
 *       this.hikariPool = hikariPool;
 *     }
 *
 *     {@literal @}Override
 *     public String getName() {
 *       return "my-health-check";
 *     }
 *
 *     {@literal @}Override
 *     public HikariPool getHikariPool() {
 *       return hikariPool;
 *     }
 *   }
 * }
 * }</pre>
 */
public abstract class HikariHealthCheck implements HealthCheck {

  @Override
  public String getName() {
    return "hikari";
  }

  public Duration getTimeout() {
    return Duration.ofSeconds(5);
  }

  public abstract HikariPool getHikariPool();

  @Override
  public Promise<Result> check(Registry registry) {
    return Blocking.get(() -> {
        try {
          getHikariPool().getConnection(getTimeout().toMillis()).close();
          return Result.healthy();
        } catch (SQLException e) {
          return HealthCheck.Result.unhealthy(String.format("Hikari couldn't get a connection after %ss", getTimeout().getSeconds()), e);
        }
      }
    );
  }
}
