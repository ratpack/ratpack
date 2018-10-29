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

package ratpack.hikari;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import ratpack.guice.ConfigurableModule;

import javax.sql.DataSource;

/**
 * An extension module that provides a {@link DataSource} from a HikariCP JDBC connection pool.
 * <p>
 * This is a {@link ConfigurableModule}, exposing the {@link HikariConfig} type as the configuration.
 * All aspects of the connection pool can be configured through this object.
 * See {@link ConfigurableModule} for usage patterns.
 * <pre class="java">{@code
 * import ratpack.service.Service;
 * import ratpack.service.StartEvent;
 * import ratpack.exec.Blocking;
 * import ratpack.guice.Guice;
 * import ratpack.hikari.HikariModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import javax.sql.DataSource;
 * import java.sql.Connection;
 * import java.sql.PreparedStatement;
 * import java.sql.ResultSet;
 *
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   static class InitDb implements Service {
 *     public void onStart(StartEvent startEvent) throws Exception {
 *       DataSource dataSource = startEvent.getRegistry().get(DataSource.class);
 *       try (Connection connection = dataSource.getConnection()) {
 *         connection.createStatement().executeUpdate("create table if not exists val(ID INT PRIMARY KEY, val VARCHAR(255));");
 *       }
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b -> b
 *         .module(HikariModule.class, hikariConfig -> {
 *           hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
 *           hikariConfig.addDataSourceProperty("URL", "jdbc:h2:mem:dev"); // Use H2 in memory database
 *         })
 *         .bind(InitDb.class)
 *       ))
 *       .handlers(chain -> chain
 *         .post("set/:val", ctx ->
 *             Blocking.get(() -> {
 *               try (Connection connection = ctx.get(DataSource.class).getConnection()) {
 *                 PreparedStatement statement = connection.prepareStatement("merge into val (id, val) key(id) values (?, ?)");
 *                 statement.setInt(1, 1);
 *                 statement.setString(2, ctx.getPathTokens().get("val"));
 *                 return statement.executeUpdate();
 *               }
 *             }).then(result ->
 *                 ctx.render(result.toString())
 *             )
 *         )
 *         .get("get", ctx ->
 *             Blocking.get(() -> {
 *               try (Connection connection = ctx.get(DataSource.class).getConnection()) {
 *                 PreparedStatement statement = connection.prepareStatement("select val from val where id = ?");
 *                 statement.setInt(1, 1);
 *                 ResultSet resultSet = statement.executeQuery();
 *                 resultSet.next();
 *                 return resultSet.getString(1);
 *               }
 *             }).then(ctx::render)
 *         )
 *       )
 *     ).test(httpClient -> {
 *       httpClient.post("set/foo");
 *       assertEquals("foo", httpClient.getText("get"));
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see <a href="http://brettwooldridge.github.io/HikariCP/" target="_blank">HikariCP</a>
 * @see HikariConfig
 * @see ConfigurableModule
 */
public class HikariModule extends ConfigurableModule<HikariConfig> {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  public HikariDataSource hikariDataSource(HikariConfig config) {
    return new HikariDataSource(config);
  }

  @Provides
  @Singleton
  public HikariPool hikariPool(HikariDataSource hikariDataSource) {
    return (HikariPool) hikariDataSource.getHikariPoolMXBean();
  }

  @Provides
  @Singleton
  public HikariService hikariService(HikariDataSource hikariDataSource) {
    return new HikariService(hikariDataSource);
  }

  @Provides
  @Singleton
  public DataSource dataSource(HikariService service) {
    return getDataSource(service);
  }

  // separate from above to allow decoration of the datasource by extending the module
  // Guice does not allow overriding @Provides methods
  protected DataSource getDataSource(HikariService service) {
    return service.getDataSource();
  }

}
