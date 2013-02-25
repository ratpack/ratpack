package org.ratpackframework.groovy.config.internal;

import com.google.inject.Module;
import org.ratpackframework.assets.StaticAssetsConfig;
import org.ratpackframework.config.*;
import org.ratpackframework.groovy.app.RoutingConfig;
import org.ratpackframework.groovy.config.Config;
import org.ratpackframework.groovy.templating.TemplatingConfig;
import org.ratpackframework.session.SessionCookieConfig;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class DefaultConfig implements Config {

  private final AddressConfig deployment = new AddressConfig();

  private final StaticAssetsConfig staticAssets;
  private final TemplatingConfig templating;
  private final RoutingConfig routing;
  private final SessionCookieConfig sessionCookie = new SessionCookieConfig();

  private final List<Module> modules = new LinkedList<>();

  public DefaultConfig(File baseDir) {
    staticAssets = new StaticAssetsConfig(new File(baseDir, "public"));
    templating = new TemplatingConfig(new File(baseDir, "templates"));
    routing = new RoutingConfig(new File(baseDir, "ratpack.groovy"));
  }

  public AddressConfig getDeployment() {
    return deployment;
  }

  public StaticAssetsConfig getStaticAssets() {
    return staticAssets;
  }

  public TemplatingConfig getTemplating() {
    return templating;
  }

  public RoutingConfig getRouting() {
    return routing;
  }

  public SessionCookieConfig getSessionCookie() {
    return sessionCookie;
  }

  public List<Module> getModules() {
    return modules;
  }
}
