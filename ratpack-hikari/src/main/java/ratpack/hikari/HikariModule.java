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

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ratpack.launch.LaunchConfig;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * An extension module that provides support for HikariCP JDBC connection pool.
 * <p>
 * This module provides a {@code DataSource} instance that is configured based on module's constructor arguments,
 * configuration properties or both.
 * </p>
 * <p>
 * Different constructor variants allow you to configure {@code dataSourceClassName} (note that HikariCP uses {@code javax.sql.DataSource} instances
 * instead of {@code java.sql.Driver} used by other connection pools), {@code minimumIdle} and {@code maximumPoolSize} as well as {@code dataSourceProperties}.
 * </p>
 * <p>
 * If you wish to configure the module using configuration properties you should use the following property names: {@code other.hikari.dataSourceClassName},
 * {@code other.hikari.minimumIdle} and {@code other.hikari.maximumPoolSize}. All configuration properties prefixed with {@code other.hikari.dataSourceProperties} will
 * be used as data source properties - e.g. {@code other.hikari.URL} will be used to set {@code URL} property on the data source.
 * </p>
 * <pre class="java">{@code
 * import com.google.common.collect.ImmutableMap;
 * import ratpack.guice.Guice;
 * import ratpack.hikari.HikariModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import javax.sql.DataSource;
 * import java.sql.Connection;
 * import java.sql.PreparedStatement;
 * import java.sql.ResultSet;
 *
 * public class Example {
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(launchConfig ->
 *         Guice.builder(launchConfig)
 *           .bindings(b ->
 *               // Use H2 in memory database
 *               b.add(new HikariModule(ImmutableMap.of("URL", "jdbc:h2:mem:dev"), "org.h2.jdbcx.JdbcDataSource"))
 *           )
 *           .build(chain -> {
 *             DataSource dataSource = chain.get(DataSource.class);
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
 *       assert httpClient.getText("get").equals("foo");
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see <a href="http://brettwooldridge.github.io/HikariCP/" target="_blank">HikariCP</a>
 */
public class HikariModule extends AbstractModule {

  private final static String DEFAULT_MIN_IDLE_SIZE = "10";
  private final static String DEFAULT_MAX_POOL_SIZE = "60";
  private final static String DEFAULT_CONNECTION_TIMEOUT = "30000";

  private Integer minimumIdleSize;
  private Integer maximumPoolSize;
  private Long connectionTimeout;
  private String dataSourceClassName;
  private Map<String, String> dataSourceProperties;

  public HikariModule() {
    this(Maps.newHashMap(), null);
  }

  public HikariModule(Map<String, String> dataSourceProperties, String dataSourceClassName) {
    this(dataSourceProperties, dataSourceClassName, null, null, null);
  }

  public HikariModule(Map<String, String> dataSourceProperties, String dataSourceClassName, Integer minimumIdleSize, Integer maximumPoolSize, Long connectionTimeout) {
    this.dataSourceProperties = dataSourceProperties;
    this.dataSourceClassName = dataSourceClassName;
    this.minimumIdleSize = minimumIdleSize;
    this.maximumPoolSize = maximumPoolSize;
    this.connectionTimeout = connectionTimeout;
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public HikariConfig hikariConfig(LaunchConfig launchConfig) {
    int maxSize = maximumPoolSize == null ? Integer.parseInt(launchConfig.getOther("hikari.maximumPoolSize", DEFAULT_MAX_POOL_SIZE)) : maximumPoolSize;
    int minSize = minimumIdleSize == null ? Integer.parseInt(launchConfig.getOther("hikari.minimumIdle", DEFAULT_MIN_IDLE_SIZE)) : minimumIdleSize;
    long connTimeout = connectionTimeout == null ? Long.parseLong(launchConfig.getOther("hikari.connectionTimeout", DEFAULT_CONNECTION_TIMEOUT)) : connectionTimeout;
    String className = dataSourceClassName == null ? launchConfig.getOther("hikari.dataSourceClassName", null) : dataSourceClassName;

    Properties properties = new Properties();
    properties.putAll(dataSourceProperties);
    properties.putAll(launchConfig.getOtherPrefixedWith("hikari.dataSourceProperties."));

    HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(maxSize);
    config.setMinimumIdle(minSize);
    config.setDataSourceClassName(className);
    config.setDataSourceProperties(properties);
    config.setConnectionTimeout(connTimeout);
    return config;
  }

  @Provides
  @Singleton
  public DataSource dataSource(HikariConfig config) {
    return new HikariDataSource(config);
  }
}
