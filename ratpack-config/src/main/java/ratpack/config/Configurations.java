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

/**
 * Builder class for creating application configuration by composing multiple sources.
 *
 * <pre class="java">{@code
 * import com.google.common.base.Charsets;
 * import com.google.common.io.Resources;
 * import ratpack.config.ConfigurationData;
 * import ratpack.config.Configurations;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.launch.RatpackLauncher;
 * import ratpack.launch.ServerConfig;
 * import ratpack.server.RatpackServer;
 *
 * import java.net.URL;
 * import java.util.Properties;
 *
 * public class Example {
 *   public static class MyHandler implements Handler {
 *     public void handle(Context context) {
 *       context.getResponse().send("Hi, my name is " + context.get(MyAppConfig.class).getName());
 *     }
 *   }
 *
 *   public static class MyAppConfig {
 *     private String name;
 *
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     Properties myData = new Properties();
 *     myData.put("server.port", "5060");
 *     myData.put("app.name", "Luke");
 *     ConfigurationData configData = Configurations.config().props(myData).sysProps().build();
 *     RatpackServer server = RatpackLauncher.with(configData.get("/server", ServerConfig.class))
 *       .registry(r -> {
 *         r.add(MyAppConfig.class, configData.get("/app", MyAppConfig.class));
 *       }).build(registry -> new MyHandler());
 *     server.start();
 *     System.out.println(server.getBindPort());
 *     System.out.println(Resources.toString(new URL("http://localhost:5060"), Charsets.UTF_8));
 *     assert server.isRunning();
 *     assert server.getBindPort() == 5060;
 *     assert "Hi, my name is Luke".equals(Resources.toString(new URL("http://localhost:5060"), Charsets.UTF_8));
 *     server.stop();
 *   }
 * }
 * }</pre>
 */
public class Configurations {
  /**
   * Begins building a new application configuration using a default object mapper.
   *
   * @return the new configuration data spec
   */
  public static ConfigurationDataSpec config() {
    return new DefaultConfigurationDataSpec();
  }

  /**
   * Begins building a new application configuration using a specified object mapper.
   *
   * @param objectMapper the object mapper to use for configuration purposes
   * @return the new configuration data spec
   */
  public static ConfigurationDataSpec config(ObjectMapper objectMapper) {
    return new DefaultConfigurationDataSpec(objectMapper);
  }
}
