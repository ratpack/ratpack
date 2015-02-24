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

package ratpack.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.config.internal.DefaultConfigDataSpec;
import ratpack.func.Action;
import ratpack.server.*;

import java.util.List;

/**
 * Configuration data for the application, potentially built from many sources.
 * <p>
 * A {@link ConfigData} object allows configuration to be “read” as Java objects.
 * The data used to populate the objects is specified when building the configuration data.
 * The static methods of this interface can be used to build a configuration data object.
 * <pre class="java">{@code
 * import com.google.common.collect.ImmutableMap;
 * import ratpack.config.ConfigData;
 * import ratpack.server.RatpackServer;
 * import ratpack.test.http.TestHttpClient;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static class MyAppConfig {
 *     private String name;
 *
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     RatpackServer server = RatpackServer.of(spec -> {
 *       ConfigData config = ConfigData.of(d -> d
 *         .props(ImmutableMap.of("server.port", "5060", "app.name", "Ratpack"))
 *         .sysProps()
 *       );
 *       return spec
 *         .serverConfig(config.getServerConfig())
 *         .registryOf(r -> r
 *           .add(config)
 *           .add(config.get("/app", MyAppConfig.class))
 *         )
 *         .handler(registry ->
 *           (ctx) -> ctx.render("Hi, my name is " + ctx.get(MyAppConfig.class).getName())
 *         );
 *     });
 *     server.start();
 *
 *     assertTrue(server.isRunning());
 *     assertEquals(5060, server.getBindPort());
 *
 *     TestHttpClient httpClient = TestHttpClient.testHttpClient(server);
 *     assertEquals("Hi, my name is Ratpack", httpClient.getText());
 *
 *     server.stop();
 *   }
 * }
 * }</pre>
 * <h3>Configuration Reloading</h3>
 * <p>
 * The created configuration data instance should be added to the server registry (as in the above example).
 * This enables automatically reloading the configuration when it changes.
 *
 * @see ConfigDataSpec
 */
public interface ConfigData extends ReloadInformant, Service {

  /**
   * Begins building a new application configuration using a default object mapper.
   *
   * @return the new configuration data spec
   */
  static ConfigDataSpec of() {
    return new DefaultConfigDataSpec(ServerEnvironment.env());
  }

  static ConfigData of(Action<? super ConfigDataSpec> config) throws Exception {
    return config.with(of()).build();
  }

  /**
   * Begins building a new application configuration using a default object mapper with the supplied modules registered.
   *
   * @param modules the Jackson modules to register with a default object mapper
   * @return the new configuration data spec
   */
  static ConfigDataSpec of(Module... modules) {
    return new DefaultConfigDataSpec(ServerEnvironment.env(), modules);
  }

  static ConfigData of(List<Module> modules, Action<? super ConfigDataSpec> config) throws Exception {
    return config.with(of(modules.toArray(new Module[modules.size()]))).build();
  }

  /**
   * Begins building a new application configuration using a specified object mapper.
   *
   * @param objectMapper the object mapper to use for configuration purposes
   * @return the new configuration data spec
   */
  static ConfigDataSpec of(ObjectMapper objectMapper) {
    return new DefaultConfigDataSpec(ServerEnvironment.env(), objectMapper);
  }

  static ConfigData of(ObjectMapper objectMapper, Action<? super ConfigDataSpec> config) throws Exception {
    return config.with(of(objectMapper)).build();
  }

  /**
   * Binds a segment of the configuration data to the specified type.
   *
   * @param pointer a <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a> specifying
   * the point in the configuration data to bind from
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return an instance of the specified type with bound configuration data
   */
  <O> O get(String pointer, Class<O> type);

  /**
   * Binds the root of the configuration data to the specified type.
   *
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return an instance of the specified type with bound configuration data
   */
  default <O> O get(Class<O> type) {
    return get(null, type);
  }

  default ServerConfig getServerConfig() {
    return getServerConfig("/server");
  }

  default ServerConfig getServerConfig(String pointer) {
    return get(pointer, ServerConfig.class);
  }

}
