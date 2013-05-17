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
import org.ratpackframework.groovy.bootstrap.GroovyKitAppFactory;
import org.ratpackframework.groovy.internal.ScriptBackedApp;
import org.ratpackframework.routing.Handler;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public abstract class RatpackScriptApp {

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
