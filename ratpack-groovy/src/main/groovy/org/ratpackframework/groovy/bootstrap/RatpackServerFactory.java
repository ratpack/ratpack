package org.ratpackframework.groovy.bootstrap;

import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.ratpackframework.assets.StaticAssetsModule;
import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.config.AddressConfig;
import org.ratpackframework.groovy.app.RoutingScriptModule;
import org.ratpackframework.groovy.config.Config;
import org.ratpackframework.groovy.config.internal.ConfigLoader;
import org.ratpackframework.groovy.templating.TemplatingModule;
import org.ratpackframework.session.SessionModule;

import java.io.File;

public class RatpackServerFactory {

  public RatpackServer create(File configScript) throws Exception {
    return create(new ConfigLoader().load(configScript));
  }

  public RatpackServer create(Config config) {
    AddressConfig addressConfig = config.getDeployment();

    org.ratpackframework.bootstrap.RatpackServerFactory ratpackServerFactory = new org.ratpackframework.bootstrap.RatpackServerFactory(
        config.getBaseDir(), addressConfig.getPort(),
        addressConfig.getBindHost(), addressConfig.getPublicHost()
    );

    StaticAssetsModule staticAssetsModule = new StaticAssetsModule(config.getStaticAssets());
    SessionModule sessionModule = new SessionModule(config.getSessionCookie());
    RoutingScriptModule routingScriptModule = new RoutingScriptModule(config.getRouting());
    TemplatingModule templatingModule = new TemplatingModule(config.getTemplating());

    Module collapsed = Modules.override(
        routingScriptModule, staticAssetsModule, sessionModule, templatingModule
    ).with(config.getModules());

    return ratpackServerFactory.create(collapsed);
  }

}
