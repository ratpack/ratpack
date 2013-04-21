package org.ratpackframework.groovy

import groovy.transform.CompileStatic
import org.ratpackframework.Factory
import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.bootstrap.RatpackServerBuilder
import org.ratpackframework.groovy.app.internal.ClosureAppFactory
import org.ratpackframework.groovy.app.internal.GroovyRatpackAppFactory
import org.ratpackframework.groovy.app.internal.ScriptBackedApp
import org.ratpackframework.http.CoreHttpHandlers
import org.ratpackframework.http.internal.FactoryBackedCoreHttpHandlers

@CompileStatic
abstract class RatpackScriptApp {

  static RatpackServer ratpack(File script) {
    String portString = System.getProperty("ratpack.port", new Integer(RatpackServerBuilder.DEFAULT_PORT).toString());
    int port = Integer.valueOf(portString);

    String host = System.getProperty("ratpack.host", null);
    boolean reloadable = Boolean.parseBoolean(System.getProperty("ratpack.reloadable", "false"));
    boolean compileStatic = Boolean.parseBoolean(System.getProperty("ratpack.compileStatic", "false"));

    ratpack(script, port, host, compileStatic, reloadable)
  }

  static RatpackServer ratpack(File script, int port, String host, boolean compileStatic, boolean reloadable) {
    File templates = new File(script.parentFile, "templates")
    ClosureAppFactory appFactory = new GroovyRatpackAppFactory(templates)

    Factory<CoreHttpHandlers> scriptBackedApp = new ScriptBackedApp(script, appFactory, compileStatic, reloadable)
    CoreHttpHandlers handlers = new FactoryBackedCoreHttpHandlers(scriptBackedApp)

    RatpackServerBuilder builder = new RatpackServerBuilder(handlers)
    builder.setPort(port)
    builder.setHost(host)

    builder.build()
  }

}
