package org.ratpackframework.groovy

import groovy.transform.CompileStatic
import org.ratpackframework.bootstrap.RatpackServer

@CompileStatic
abstract class RatpackScriptApp {

  static RatpackServer ratpack(File script) {
//    String portString = System.getProperty("ratpack.port", new Integer(RatpackServerBuilder.DEFAULT_PORT).toString());
//    int port = Integer.valueOf(portString);
//
//    String host = System.getProperty("ratpack.host", null);
//    boolean reloadable = Boolean.parseBoolean(System.getProperty("ratpack.reloadable", "false"));
//    boolean compileStatic = Boolean.parseBoolean(System.getProperty("ratpack.compileStatic", "false"));
//
//    ratpack(script, port, host, compileStatic, reloadable)
  }

  static RatpackServer ratpack(File script, int port, String host, boolean compileStatic, boolean reloadable) {
//    Handler scriptBackedApp = new ScriptBackedApp(script, new GroovyHandlerFactory(), compileStatic, reloadable)
//
//    RatpackServerBuilder builder = new RatpackServerBuilder(scriptBackedApp)
//    builder.setPort(port)
//    builder.setHost(host)
//
//    builder.build()
  }

}
