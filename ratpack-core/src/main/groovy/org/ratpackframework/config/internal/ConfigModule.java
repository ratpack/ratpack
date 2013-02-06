package org.ratpackframework.config.internal;

import com.google.inject.AbstractModule;
import org.ratpackframework.config.*;

public class ConfigModule extends AbstractModule {

  private final Config config;

  public ConfigModule(Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(LayoutConfig.class).toInstance(config.getLayout());
    bind(DeploymentConfig.class).toInstance(config.getDeployment());
    bind(StaticAssetsConfig.class).toInstance(config.getStaticAssets());
    bind(TemplatingConfig.class).toInstance(config.getTemplating());
    bind(RoutingConfig.class).toInstance(config.getRouting());
    bind(SessionCookieConfig.class).toInstance(config.getSessionCookie());
  }
}
