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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ratpack.launch.LaunchConfig;

import javax.sql.DataSource;
import java.util.HashMap;
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
 * <p>
 * Example usage: (Java DSL)
 * </p>
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.guice.*;
 * import ratpack.func.Action;
 * import ratpack.launch.*;
 * import ratpack.hikari.HikariModule;
 * import com.google.common.collect.ImmutableMap;
 *
 * class ModuleBootstrap implements Action&lt;BindingsSpec&gt; {
 *   public void execute(BindingsSpec bindings) {
 *     Map dataSourceProperties = ImmutableMap.of("URL", "jdbc:h2:mem:dev");
 *     bindings.add(new HikariModule(dataSourceProperties, "org.h2.jdbcx.JdbcDataSource"));
 *   }
 * }
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
 *   .build(new HandlerFactory() {
 *     public Handler create(LaunchConfig launchConfig) {
 *       return Guice.chain(launchConfig, new ModuleBootstrap(), new Action&lt;Chain&gt;() {
 *         public void execute(Chain chain) {
 *           //...
 *         }
 *       });
 *     }
 *   });
 *
 * launchConfig.execController.close()
 * </pre>
 * <p>
 * Example usage: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.hikari.HikariModule
 * import groovy.sql.Sql
 * import ratpack.groovy.sql.SqlModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new SqlModule(),
 *             new HikariModule("org.h2.jdbcx.JdbcDataSource", URL: "jdbc:h2:mem:dev", user: 'user', password: 'pass')
 *   }
 *
 *   handlers { Sql sql -&gt;
 *     get('schemas') {
 *       def schemas = sql.rows('show schemas')
 *       render schemas*.getAt(0).join(', ')
 *     }
 *   }
 * }
 *
 * </pre>
 *
 * @see <a href="http://brettwooldridge.github.io/HikariCP/" target="_blank">HikariCP</a>
 */
public class HikariModule extends AbstractModule {

  private final static String DEFAULT_MIN_IDLE_SIZE = "10";
  private final static String DEFAULT_MAX_POOL_SIZE = "60";

  private Integer minimumIdleSize;
  private Integer maximumPoolSize;
  private String dataSourceClassName;
  private Map<String, String> dataSourceProperties;

  public HikariModule() {
    this(new HashMap<String, String>(), null);
  }

  public HikariModule(Map<String, String> dataSourceProperties, String dataSourceClassName) {
    this(dataSourceProperties, dataSourceClassName, null, null);
  }

  public HikariModule(Map<String, String> dataSourceProperties, String dataSourceClassName, Integer minimumIdleSize, Integer maximumPoolSize) {
    this.dataSourceProperties =dataSourceProperties;
    this.dataSourceClassName = dataSourceClassName;
    this.minimumIdleSize = minimumIdleSize;
    this.maximumPoolSize = maximumPoolSize;
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public HikariConfig hikariConfig(LaunchConfig launchConfig) {
    int maxSize = maximumPoolSize == null ? Integer.parseInt(launchConfig.getOther("hikari.maximumPoolSize", DEFAULT_MAX_POOL_SIZE)) : maximumPoolSize;
    int minSize = minimumIdleSize == null ? Integer.parseInt(launchConfig.getOther("hikari.minimumIdle", DEFAULT_MIN_IDLE_SIZE)) : minimumIdleSize;
    String className = dataSourceClassName == null ? launchConfig.getOther("hikari.dataSourceClassName", null) : dataSourceClassName;

    Properties properties = new Properties();
    properties.putAll(dataSourceProperties);
    properties.putAll(launchConfig.getOtherPrefixedWith("hikari.dataSourceProperties."));

    HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(maxSize);
    config.setMinimumIdle(minSize);
    config.setDataSourceClassName(className);
    config.setDataSourceProperties(properties);
    return config;
  }

  @Provides
  @Singleton
  public DataSource dataSource(HikariConfig config) {
    return new HikariDataSource(config);
  }
}
