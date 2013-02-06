package org.ratpackframework.config.internal;

import com.google.inject.Module;
import org.ratpackframework.config.*;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class DefaultConfig implements Config {

  private final LayoutConfig layout;
  private final DeploymentConfig deployment = new DeploymentConfig();
  private final StaticAssetsConfig staticAssets = new StaticAssetsConfig();
  private final TemplatingConfig templating = new TemplatingConfig();
  private final RoutingConfig routing = new RoutingConfig();
  private final SessionCookieConfig sessionCookie = new SessionCookieConfig();

  private final List<Module> modules = new LinkedList<>();

  public DefaultConfig(File baseDir) {
    layout = new LayoutConfig(baseDir);
  }

  public LayoutConfig getLayout() {
    return layout;
  }

  public DeploymentConfig getDeployment() {
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
