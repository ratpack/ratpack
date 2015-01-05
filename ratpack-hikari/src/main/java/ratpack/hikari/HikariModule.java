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
import ratpack.guice.ConfigurableModule;

import javax.sql.DataSource;

/**
 * An extension module that provides a {@link DataSource} from a HikariCP JDBC connection pool.
 * <p>
 * This is a {@link ConfigurableModule}, exposing the {@link HikariConfig} type as the configuration.
 * All aspects of the connection pool can be configured through this object.
 * See {@link ConfigurableModule} for usage patterns.
 * <pre class="java">{@code
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
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(registry ->
 *         Guice.builder(registry)
 *           .bindings(b ->
 *               b.add(HikariModule.class, hikariConfig -> {
 *                 hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
 *                 hikariConfig.addDataSourceProperty("URL", "jdbc:h2:mem:dev"); // Use H2 in memory database
 *               })
 *           )
 *           .build(chain -> {
 *             DataSource dataSource = chain.getRegistry().get(DataSource.class);
 *             try (Connection connection = dataSource.getConnection()) {
 *               connection.createStatement().executeUpdate("create table if not exists val(ID INT PRIMARY KEY, val VARCHAR(255));");
 *             }
 *
 *             chain
 *               .post("set/:val", ctx ->
 *                   ctx.blocking(() -> {
 *                     try (Connection connection = dataSource.getConnection()) {
 *                       PreparedStatement statement = connection.prepareStatement("merge into val (id, val) key(id) values (?, ?)");
 *                       statement.setInt(1, 1);
 *                       statement.setString(2, ctx.getPathTokens().get("val"));
 *                       return statement.executeUpdate();
 *                     }
 *                   }).then(result ->
 *                       ctx.render(result.toString())
 *                   )
 *               )
 *               .get("get", ctx ->
 *                   ctx.blocking(() -> {
 *                     try (Connection connection = dataSource.getConnection()) {
 *                       PreparedStatement statement = connection.prepareStatement("select val from val where id = ?");
 *                       statement.setInt(1, 1);
 *                       ResultSet resultSet = statement.executeQuery();
 *                       resultSet.next();
 *                       return resultSet.getString(1);
 *                     }
 *                   }).then(ctx::render)
 *               );
 *           })
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
  public DataSource dataSource(HikariConfig config) {
    return new HikariDataSource(config);
  }

}
