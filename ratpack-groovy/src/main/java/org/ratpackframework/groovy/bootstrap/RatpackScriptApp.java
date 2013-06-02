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

package org.ratpackframework.groovy.bootstrap;

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
 * Script apps are based on a Groovy script, that ultimately calls the {@link org.ratpackframework.groovy.RatpackScript#ratpack(groovy.lang.Closure)} method.
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

  /**
   * Constants for the names of configuration properties.
   *
   * @see #ratpack(java.io.File, java.util.Properties)
   */
  public final class Property {
    private Property() {}

    /**
     * The port to listen for requests on. Defaults to {@link RatpackServerBuilder#DEFAULT_PORT}.
     * <p>
     * <b>Value:</b> {@value}
     */
    public static final String PORT = "ratpack.port";

    /**
     * The address to bind to. Defaults to {@code null} (all addresses).
     * <p>
     * If the value is not {@code null}, it will converted to an Inet Address via {@link InetAddress#getByName(String)}.
     * <p>
     * <b>Value:</b> {@value} - (inet address)
     */
    public static final String ADDRESS = "ratpack.address";

    /**
     * Whether to reload the application if the script changes at runtime. Defaults to {@code false}.
     * <p>
     * <b>Value:</b> {@value} - (boolean)
     */
    public static final String RELOADABLE = "ratpack.reloadable";

    /**
     * Whether to compile the script statically. Defaults to {@code false}.
     *
     * <p>
     * <b>Value:</b> {@value} - (boolean)
     */
    public static final String COMPILE_STATIC = "ratpack.compileStatic";
  }

  private RatpackScriptApp() {}

  /**
   * Calls {@link #ratpack(java.io.File, java.util.Properties)} with the given script and {@link System#getProperties()} as the properties argument.
   *
   * @param script The script that defines the ratpack application
   * @return A not yet started {@link RatpackServer}
   * @see #ratpack(java.io.File, java.util.Properties)
   */
  public static RatpackServer ratpack(File script) {
    return ratpack(script, System.getProperties());
  }

  /**
   * Creates a script based Ratpack app, using the given properties as source for the configuration.
   * <p>
   * See {@link Property} for the available properties and their default values and types.
   * <p>
   * Values are unpacked from the given properties, then used to call {@link #ratpack(java.io.File, java.io.File, int, java.net.InetAddress, boolean, boolean)}.
   * There is no property for the {@code baseDir} property. The directory that contains the script will be the base directory.
   *
   * @param script The script file for the application definition
   * @param properties The configuration properties
   * @return A not yet started {@link RatpackServer}
   * @see Property
   * @see #ratpack(java.io.File, java.io.File, int, java.net.InetAddress, boolean, boolean)
   */
  public static RatpackServer ratpack(File script, Properties properties) {
    String portString = properties.getProperty(Property.PORT, new Integer(RatpackServerBuilder.DEFAULT_PORT).toString());
    int port = Integer.valueOf(portString);

    InetAddress address = null;
    String addressString = properties.getProperty(Property.ADDRESS);

    if (addressString != null) {
      try {
        address = InetAddress.getByName(addressString);
      } catch (UnknownHostException e) {
        throw new IllegalStateException("Failed to resolve requested bind address: " + addressString, e);
      }
    }

    boolean reloadable = Boolean.parseBoolean(properties.getProperty(Property.RELOADABLE, "false"));
    boolean compileStatic = Boolean.parseBoolean(properties.getProperty(Property.COMPILE_STATIC, "false"));

    return ratpack(script, script.getAbsoluteFile().getParentFile(), port, address, compileStatic, reloadable);
  }

  /**
   * Creates a new Ratpack server.
   * <h5>Reloading Support</h5>
   * <p>
   * If the {@code reloadable} argument is true, changes to the script at runtime will be effectual.
   * On each request, the script is checked for changes.
   * If a change is detected, the entire application is reloaded (but the server is not stop/started).
   * This includes <b>all modules</b> and objects.
   * <p>
   * Reloading support should not be turned on for production deployments as it has considerable overhead for each request.
   * </p>
   *
   * @param script The script file defining the application
   * @param baseDir The file system root of the app, used for the default {@link org.ratpackframework.file.FileSystemBinding}
   * @param port The port to listen for request on
   * @param address The address to listen for requests on
   * @param compileStatic Whether or not to compile the script statically
   * @param reloadable Whether or not to watch for changes to the script at runtime, and reload the application accordingly
   * @return A not yet started Ratpack server
   */
  public static RatpackServer ratpack(File script, File baseDir, int port, InetAddress address, boolean compileStatic, boolean reloadable) {
    Handler scriptBackedApp = new ScriptBackedApp(script, new GroovyKitAppFactory(), compileStatic, reloadable);

    RatpackServerBuilder builder = new RatpackServerBuilder(scriptBackedApp, baseDir);
    builder.setPort(port);
    builder.setAddress(address);

    return builder.build();
  }

}
