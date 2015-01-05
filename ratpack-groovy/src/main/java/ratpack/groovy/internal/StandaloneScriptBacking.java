/*
 * Copyright 2013 the original author or authors.
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

package ratpack.groovy.internal;

import com.google.common.base.StandardSystemProperty;
import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import ratpack.api.Nullable;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.func.Action;
import ratpack.groovy.launch.GroovyScriptFileHandlerFactory;
import ratpack.groovy.launch.internal.GroovyClosureHandlerFactory;
import ratpack.groovy.launch.internal.GroovyVersionCheck;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigs;
import ratpack.launch.internal.DelegatingLaunchConfig;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfigBuilder;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class StandaloneScriptBacking implements Action<Closure<?>> {

  private final static AtomicReference<Action<? super RatpackServer>> CAPTURE_ACTION = new AtomicReference<>(null);


  public static void captureNext(Action<? super RatpackServer> action) {
    CAPTURE_ACTION.set(action);
  }

  //TODO-JOHN
  public void execute(final Closure<?> closure) throws Exception {
    GroovyVersionCheck.ensureRequiredVersionUsed(GroovySystem.getVersion());

    Path scriptFile = ClosureUtil.findScript(closure);

    Properties defaultProperties = new Properties();
    Path baseDir;

    if (scriptFile == null) {
      baseDir = new File(StandardSystemProperty.USER_DIR.value()).toPath();
    } else {
      baseDir = scriptFile.getParent();
    }

    Properties properties = createProperties(scriptFile);

    Path configFile = new DefaultFileSystemBinding(baseDir).file(LaunchConfigs.CONFIG_RESOURCE_DEFAULT);
    //TODO-JOHN
    LaunchConfig launchConfig = LaunchConfigs.createFromFile(closure.getClass().getClassLoader(), baseDir, configFile, properties, defaultProperties);

    if (scriptFile == null) {
      launchConfig = new DelegatingLaunchConfig(launchConfig) {
        @Override
        public HandlerFactory getHandlerFactory() {
          return new GroovyClosureHandlerFactory(closure);
        }
      };
    }

    final LaunchConfig effectiveLaunchConfig = launchConfig;
    RatpackServer server = RatpackServer.with(ServerConfigBuilder.launchConfig(effectiveLaunchConfig).build())
      .build(r -> effectiveLaunchConfig.getHandlerFactory().create(r));

    Action<? super RatpackServer> action = CAPTURE_ACTION.getAndSet(null);
    if (action != null) {
      action.execute(server);
    }

    server.start();

    try {
      while (server.isRunning() && !Thread.interrupted()) {
        Thread.sleep(1000);
      }
    } catch (InterruptedException ignore) {
      // do nothing
    }

    server.stop();
  }

  protected Properties createProperties(@Nullable Path scriptFile) {
    Properties properties = LaunchConfigs.getDefaultPrefixedProperties();

    properties.setProperty(LaunchConfigs.Property.HANDLER_FACTORY, GroovyScriptFileHandlerFactory.class.getName());
    properties.setProperty(LaunchConfigs.Property.DEVELOPMENT, "true");

    if (scriptFile != null) {
      properties.setProperty("other." + GroovyScriptFileHandlerFactory.SCRIPT_PROPERTY_NAME, scriptFile.getFileName().toString());
    }

    return properties;
  }

}
