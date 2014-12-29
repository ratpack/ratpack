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

package ratpack.configuration;

import ratpack.configuration.internal.ClasspathBackedConfiguration;
import ratpack.configuration.internal.EnvironmentVariablesBackedConfiguration;
import ratpack.configuration.internal.FileBackedConfiguration;
import ratpack.configuration.internal.SystemPropertiesBackedConfiguration;

/**
 * Builder class for creating application configuration by composing multiple sources.
 *
 * <pre class="java">{@code
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.server.RatpackServer;
 * import ratpack.launch.RatpackLauncher;
 * import ratpack.launch.ServerConfig;
 * import ratpack.configuration.Configuration;
 * import ratpack.configuration.Configurations;
 *
 * public class Example {
 *  public static class MyHandler implements Handler {
 *    public void handle(Context context) throws Exception {
 *    }
 *  }
 *
 *  public static class MyAppConfig {
 *    private MyHandler myHandler;
 *
 *    public void setMyHandler(MyHandler myHandler) {
 *      this.myHandler = myHandler;
 *    }
 *
 *    public MyHandler getMyHandler() {
 *      return myHandler;
 *    }
 *  }
 *
 *  public static void main(String[] args) throws Exception {
 *    Configuration configuration = Configurations.load("classpath-properties.properties").sysProps().env().fileSystem("/tmp/default.properties").build();
 *    RatpackServer server = RatpackLauncher.launcher(r -> {
 *      //Example adding configuration to registry
 *      //r.add(ServerConfig.class, configuration.toClass(ServerConfig.class));
 *      //r.add(MyAppConfig.class, configuration.toClass(MyAppConfig.class));
 *    }).build(registry -> {
 *      //This should be how we return the handler
 *      //return registry.get(MyAppConfig.class).getMyHandler();
 *      return new MyHandler();
 *    });
 *    server.start();
 *
 *    assert server.isRunning();
 *
 *    server.stop();
 *  }
 * }
 * }</pre>
 */
public class Configurations {

  public static ConfigurationSpec load(String path) {
    return new ClasspathBackedConfiguration(path);
  }

  public static ConfigurationSpec sysProps() {
    return new SystemPropertiesBackedConfiguration();
  }

  public static ConfigurationSpec env() {
    return new EnvironmentVariablesBackedConfiguration();
  }

  public static ConfigurationSpec fileSystem(String path) {
    return new FileBackedConfiguration(path);
  }
}
