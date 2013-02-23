package org.ratpackframework.groovy;

import com.google.inject.Module;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.assets.StaticAssetsConfig;
import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.config.AddressConfig;
import org.ratpackframework.groovy.app.RoutingConfig;
import org.ratpackframework.groovy.bootstrap.RatpackServerFactory;
import org.ratpackframework.groovy.config.Config;
import org.ratpackframework.groovy.config.internal.DefaultConfig;
import org.ratpackframework.groovy.templating.TemplatingConfig;
import org.ratpackframework.session.SessionCookieConfig;

import java.io.File;
import java.util.List;

public class Ratpack implements Config {

  private final File baseDir;
  private final DefaultConfig config;

  public Ratpack(File baseDir) {
    this.baseDir = baseDir;
    this.config = new DefaultConfig(baseDir);
  }

  public static RatpackServer ratpack() {
    return ratpack((Closure<?>) null);
  }

  public static RatpackServer ratpack(@DelegatesTo(Ratpack.class) Closure<?> closure) {
    return ratpack(new File(System.getProperty("user.dir")), closure);
  }

  public static RatpackServer ratpack(File baseDir) {
    return ratpack(baseDir, null);
  }

  public static RatpackServer ratpack(File baseDir, @DelegatesTo(Ratpack.class) Closure<?> closure) {
    Ratpack ratpack = new Ratpack(baseDir);
    if (closure != null) {
      Closure<?> clone = (Closure<?>) closure.clone();
      clone.setDelegate(ratpack);
      clone.call();
    }
    RatpackServer server = new RatpackServerFactory().create(ratpack);
    server.startAndWait();
    return server;
  }

  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public AddressConfig getDeployment() {
    return config.getDeployment();
  }

  @Override
  public StaticAssetsConfig getStaticAssets() {
    return config.getStaticAssets();
  }

  @Override
  public TemplatingConfig getTemplating() {
    return config.getTemplating();
  }

  @Override
  public RoutingConfig getRouting() {
    return config.getRouting();
  }

  @Override
  public SessionCookieConfig getSessionCookie() {
    return config.getSessionCookie();
  }

  @Override
  public List<Module> getModules() {
    return config.getModules();
  }

}
