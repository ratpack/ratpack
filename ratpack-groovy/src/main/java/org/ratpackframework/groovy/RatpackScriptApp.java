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

package org.ratpackframework.groovy;

import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.bootstrap.RatpackServerBuilder;
import org.ratpackframework.groovy.bootstrap.internal.GroovyKitAppFactory;
import org.ratpackframework.groovy.internal.ScriptBackedApp;
import org.ratpackframework.handling.Handler;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Factory methods for starting Groovy script based Ratpack applications.
 * <p>
 * Script apps are based on a Groovy script, that ultimately calls the {@link RatpackScript#ratpack(groovy.lang.Closure)} method.
 * <h3>Features</h3>
 * Ratpack script apps have more features than plain Ratpack applications.
 * <h4>Guice Support</h4>
 * Script apps are Guice backed, via the {@link org.ratpackframework.guice.Guice Ratpack Guice integration}.
 * <h4>Templating Support</h4>
 * Support for Groovy based templates is provided by the {@link org.ratpackframework.groovy.templating.TemplatingModule}.
 * <h4>Session Support</h4>
 * Support for session tracking is provided by the {@link org.ratpackframework.session.SessionModule}.
 * This does not include session based storage.
 * For this, you can add the {@link org.ratpackframework.session.store.MapSessionsModule} module (or add your own storage mechanism).
 * <h4>Runtime reloading</h4>
 * See {@link #ratpack(java.io.File, java.io.File, int, java.net.InetAddress, boolean, boolean)}
 */
public abstract class RatpackScriptApp {

  private RatpackScriptApp() {}

  /**
   * Calls {@link #ratpack(java.io.File, java.util.Properties)} with the given script and {@link System#getProperties()} as the properties argument.
   *
   * @param script The script that defines the ratpack application
   * @return A not yet started {@link RatpackServer}
   */
  public static RatpackServer ratpack(File script) {
    return ratpack(script, System.getProperties());
  }

  public static RatpackServer ratpack(File script, Properties properties) {
    String portString = properties.getProperty("ratpack.port", new Integer(RatpackServerBuilder.DEFAULT_PORT).toString());
    int port = Integer.valueOf(portString);

    InetAddress address = null;
    String addressString = properties.getProperty("ratpack.address");

    if (addressString != null) {
      try {
        address = InetAddress.getByName(addressString);
      } catch (UnknownHostException e) {
        throw new IllegalStateException("Failed to resolve requested bind address: " + addressString, e);
      }
    }

    boolean reloadable = Boolean.parseBoolean(properties.getProperty("ratpack.reloadable", "false"));
    boolean compileStatic = Boolean.parseBoolean(properties.getProperty("ratpack.compileStatic", "false"));

    return ratpack(script, script.getAbsoluteFile().getParentFile(), port, address, compileStatic, reloadable);
  }

  public static RatpackServer ratpack(File script, File baseDir, int port, InetAddress address, boolean compileStatic, boolean reloadable) {
    Handler scriptBackedApp = new ScriptBackedApp(script, new GroovyKitAppFactory(), compileStatic, reloadable);

    RatpackServerBuilder builder = new RatpackServerBuilder(scriptBackedApp, baseDir);
    builder.setPort(port);
    builder.setAddress(address);

    return builder.build();
  }

}
