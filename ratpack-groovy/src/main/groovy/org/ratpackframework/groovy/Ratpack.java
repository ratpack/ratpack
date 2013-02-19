package org.ratpackframework.groovy;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.assets.StaticAssetsConfig;
import org.ratpackframework.assets.StaticAssetsModule;
import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.bootstrap.RatpackServerFactory;
import org.ratpackframework.config.AddressConfig;
import org.ratpackframework.groovy.app.ClosureRouting;
import org.ratpackframework.groovy.app.Routing;
import org.ratpackframework.groovy.templating.TemplatingConfig;
import org.ratpackframework.groovy.templating.TemplatingModule;
import org.ratpackframework.session.SessionCookieConfig;
import org.ratpackframework.session.SessionModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ratpack {

  private final StaticAssetsConfig staticAssetsConfig;
  private final TemplatingConfig templatingConfig;
  private final AddressConfig addressConfig;
  private final SessionCookieConfig sessionCookieConfig;

  private File baseDir;
  private Closure<?> routing;
  private List<Module> modules = new ArrayList<>();

  public Ratpack(File baseDir) {
    this.baseDir = baseDir;
    staticAssetsConfig = new StaticAssetsConfig(new File(baseDir, "public"));
    templatingConfig = new TemplatingConfig(new File(baseDir, "templates"));
    addressConfig = new AddressConfig();
    sessionCookieConfig = new SessionCookieConfig();
  }

  public static RatpackServer ratpack(@DelegatesTo(Ratpack.class) Closure<?> closure) {
    return ratpack(new File(System.getProperty("user.dir")), closure);
  }

  public static RatpackServer ratpack(File baseDir, @DelegatesTo(Ratpack.class) Closure<?> closure) {
    Ratpack ratpack = new Ratpack(baseDir);
    Closure<?> clone = (Closure<?>) closure.clone();
    clone.setDelegate(ratpack);
    clone.call();
    RatpackServer server = ratpack.server();
    server.startAndWait();
    return server;
  }

  public StaticAssetsConfig getStaticAssets() {
    return staticAssetsConfig;
  }

  public TemplatingConfig getTemplating() {
    return templatingConfig;
  }

  public AddressConfig getAddress() {
    return addressConfig;
  }

  public SessionCookieConfig getSessionCookie() {
    return sessionCookieConfig;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  public void routing(@DelegatesTo(Routing.class) Closure<?> routing) {
    this.routing = routing;
  }

  public void modules(Module... modules) {
    this.modules.addAll(Arrays.asList(modules));
  }

  public RatpackServer server() {
    StaticAssetsModule staticAssetsModule = new StaticAssetsModule(staticAssetsConfig);
    SessionModule sessionModule = new SessionModule(sessionCookieConfig);
    TemplatingModule templatingModule = new TemplatingModule(templatingConfig);

    Module collapsed = Modules.override(staticAssetsModule, sessionModule, templatingModule).with(modules);
    RatpackServerFactory serverFactory = new RatpackServerFactory(baseDir, addressConfig);

    return serverFactory.create(new ClosureRouting(routing), collapsed);
  }
}
