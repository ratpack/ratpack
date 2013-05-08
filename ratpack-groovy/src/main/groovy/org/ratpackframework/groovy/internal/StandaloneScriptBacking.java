package org.ratpackframework.groovy.internal;

import groovy.lang.Closure;
import org.ratpackframework.Action;
import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.groovy.RatpackScriptApp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class StandaloneScriptBacking implements Action<Closure<?>> {

  private final static AtomicReference<Action<? super RatpackServer>> captureAction = new AtomicReference<>(null);

  public static void captureNext(Action<? super RatpackServer> action) {
    captureAction.set(action);
  }

  @Override
  public void execute(Closure<?> closure) {
    File scriptFile = findScript(closure);

    Properties properties = new Properties(System.getProperties());
    properties.setProperty("ratpack.reloadable", "true");

    RatpackServer ratpack = RatpackScriptApp.ratpack(scriptFile, properties);

    Action<? super RatpackServer> action = captureAction.getAndSet(null);
    if (action != null) {
      action.execute(ratpack);
    }

    ratpack.startAndWait();
  }

  private File findScript(Closure<?> closure) {
    Class<? extends Closure> clazz = closure.getClass();
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
