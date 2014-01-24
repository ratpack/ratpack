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

import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import ratpack.api.Nullable;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.groovy.launch.GroovyClosureHandlerFactory;
import ratpack.groovy.launch.GroovyScriptFileHandlerFactory;
import ratpack.groovy.launch.GroovyVersionChecker;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigFactory;
import ratpack.launch.internal.DelegatingLaunchConfig;
import ratpack.launch.internal.LaunchConfigInternal;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerBuilder;
import ratpack.util.Action;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class StandaloneScriptBacking implements Action<Closure<?>> {

  private final static AtomicReference<Action<? super RatpackServer>> CAPTURE_ACTION = new AtomicReference<>(null);


  public static void captureNext(Action<? super RatpackServer> action) {
    CAPTURE_ACTION.set(action);
  }

  public void execute(final Closure<?> closure) throws Exception {
    GroovyVersionChecker.ensureRequiredVersionUsed(GroovySystem.getVersion());

    Path scriptFile = findScript(closure);

    Properties defaultProperties = new Properties();
    Path baseDir;

    if (scriptFile == null) {
      baseDir = new File(System.getProperty("user.dir")).toPath();
    } else {
      baseDir = scriptFile.getParent();
    }

    Properties properties = createProperties(scriptFile);

    Path configFile = new DefaultFileSystemBinding(baseDir).file(LaunchConfigFactory.CONFIG_RESOURCE_DEFAULT);
    LaunchConfig launchConfig = LaunchConfigFactory.createFromFile(closure.getClass().getClassLoader(), baseDir, configFile, properties, defaultProperties);

    if (scriptFile == null) {
      launchConfig = new DelegatingLaunchConfig((LaunchConfigInternal) launchConfig) {
        @Override
        public HandlerFactory getHandlerFactory() {
          return new GroovyClosureHandlerFactory(closure);
        }
      };
    }

    RatpackServer server = RatpackServerBuilder.build(launchConfig);

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
    Properties properties = LaunchConfigFactory.getDefaultPrefixedProperties();

    properties.setProperty(LaunchConfigFactory.Property.HANDLER_FACTORY, GroovyScriptFileHandlerFactory.class.getName());
    properties.setProperty(LaunchConfigFactory.Property.RELOADABLE, "true");

    if (scriptFile != null) {
      properties.setProperty("other." + GroovyScriptFileHandlerFactory.SCRIPT_PROPERTY_NAME, scriptFile.getFileName().toString());
    }

    return properties;
  }

  private <T> Path findScript(Closure<T> closure) throws URISyntaxException {
    Class<?> clazz = closure.getClass();
    ProtectionDomain protectionDomain = clazz.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    URL location = codeSource.getLocation();
    URI uri = location.toURI();
    Path path = Paths.get(uri);
    if (Files.exists(path)) {
      return path;
    } else {
      return null;
    }
  }
}
