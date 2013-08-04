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

package org.ratpackframework.groovy.internal;

import groovy.lang.Closure;
import org.ratpackframework.groovy.launch.GroovyScriptHandlerFactory;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.launch.LaunchConfigFactory;
import org.ratpackframework.server.RatpackServer;
import org.ratpackframework.server.RatpackServerBuilder;
import org.ratpackframework.util.Action;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class StandaloneScriptBacking implements Action<Closure<?>> {

  private final static AtomicReference<Action<? super RatpackServer>> CAPTURE_ACTION = new AtomicReference<Action<? super RatpackServer>>(null);

  public static void captureNext(Action<? super RatpackServer> action) {
    CAPTURE_ACTION.set(action);
  }

  public void execute(Closure<?> closure) {
    File scriptFile = findScript(closure);

    Properties defaultProperties = new Properties();
    defaultProperties.setProperty(LaunchConfigFactory.Property.RELOADABLE, "true");

    Properties properties = LaunchConfigFactory.getDefaultPrefixedProperties();
    properties.setProperty(LaunchConfigFactory.Property.HANDLER_FACTORY, GroovyScriptHandlerFactory.class.getName());
    properties.setProperty(GroovyScriptHandlerFactory.SCRIPT_PROPERTY_NAME, scriptFile.getName());

    File baseDir = scriptFile.getParentFile();
    File configFile = new File(baseDir, LaunchConfigFactory.CONFIG_RESOURCE_DEFAULT);

    LaunchConfig launchConfig = LaunchConfigFactory.createFromFile(closure.getClass().getClassLoader(), baseDir, configFile, properties, defaultProperties);

    RatpackServer ratpack = RatpackServerBuilder.build(launchConfig);

    Action<? super RatpackServer> action = CAPTURE_ACTION.getAndSet(null);
    if (action != null) {
      action.execute(ratpack);
    }

    try {
      ratpack.start();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to start Ratpack application", e);
    }
  }

  private <T> File findScript(Closure<T> closure) {
    Class<?> clazz = closure.getClass();
    ProtectionDomain protectionDomain = clazz.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    URL location = codeSource.getLocation();

    if (!location.getProtocol().equals("file")) {
      throw new IllegalStateException("Can not determine ratpack script from closure " + closure + " as code source location is not a file URL: " + location);
    }

    URI uri;
    try {
      uri = location.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return new File(uri);
  }
}
