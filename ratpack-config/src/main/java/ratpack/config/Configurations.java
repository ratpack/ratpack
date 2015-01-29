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

import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.config.internal.DefaultConfigurationDataSpec;
import ratpack.server.ServerEnvironment;

/**
 * Builder class for creating application configuration by composing multiple sources.
 *
 * <pre class="java">{@code
 * import com.google.common.collect.ImmutableMap;
 * import ratpack.config.ConfigurationData;
 * import ratpack.config.Configurations;
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ReloadInformant;
 * import ratpack.server.ServerConfig;
 * import ratpack.test.ServerBackedApplicationUnderTest;
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
 *     ConfigurationData configData = Configurations.config()
 *       .props(ImmutableMap.of("server.port", "5060", "app.name", "Ratpack"))
 *       .sysProps()
 *       .build();
 *
 *     RatpackServer server = RatpackServer.of(spec -> spec
 *       .config(configData.get("/server", ServerConfig.class))
 *       .registryOf(r -> r
 *         .add(ReloadInformant.class, configData.getReloadInformant())
 *         .add(MyAppConfig.class, configData.get("/app", MyAppConfig.class))
 *       )
 *       .handler(registry ->
 *         (ctx) -> ctx.render("Hi, my name is " + ctx.get(MyAppConfig.class).getName())
 *       ));
 *     server.start();
 *
 *     assertTrue(server.isRunning());
 *     assertEquals(5060, server.getBindPort());
 *
 *     TestHttpClient httpClient = TestHttpClient.testHttpClient(new ServerBackedApplicationUnderTest(() -> server));
 *     assertEquals("Hi, my name is Ratpack", httpClient.getText());
 *
 *     server.stop();
 *   }
 * }
 * }</pre>
 *
 * @see ratpack.config.ConfigurationDataSpec
 */
public class Configurations {
  /**
   * Begins building a new application configuration using a default object mapper.
   *
   * @return the new configuration data spec
   */
  public static ConfigurationDataSpec config() {
    return new DefaultConfigurationDataSpec(ServerEnvironment.env());
  }

  /**
   * Begins building a new application configuration using a specified object mapper.
   *
   * @param objectMapper the object mapper to use for configuration purposes
   * @return the new configuration data spec
   */
  public static ConfigurationDataSpec config(ObjectMapper objectMapper) {
    return new DefaultConfigurationDataSpec(ServerEnvironment.env(), objectMapper);
  }
}
