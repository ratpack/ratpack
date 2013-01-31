/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.app;

import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.RoutedRequest;
import org.ratpackframework.routing.internal.ScriptBackedRouter;
import org.ratpackframework.session.internal.DefaultSessionIdGenerator;
import org.ratpackframework.session.internal.SessionManager;
import org.ratpackframework.templating.TemplateRenderer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;

import java.io.File;

public class RatpackAppFactory {

  public RatpackApp create(Config config) {
    Vertx vertx = config.getVertx();

    File publicDir = new File(config.getBaseDir(), config.getStaticAssetsDir());

    File templatesDir = new File(config.getBaseDir(), config.getTemplatesDir());
    TemplateRenderer templateRenderer = new TemplateRenderer(vertx, templatesDir, config.getTemplatesCacheSize(), config.isStaticallyCompileTemplates());

    File routesFile = new File(config.getBaseDir(), config.getRoutes());

    SessionManager sessionManager = new SessionManager(
        new DefaultSessionIdGenerator(), config.getMaxActiveSessions(), config.getSessionTimeoutMins(),
        config.getSessionCookieExpiresMins(), config.getHost(), "/"
    );

    ResponseFactory responseFactory = new ResponseFactory(templateRenderer, sessionManager);
    Handler<RoutedRequest> router = new ScriptBackedRouter(vertx, routesFile, responseFactory, config.isStaticallyCompileRoutes(), config.isReloadRoutes());

    Handler<Vertx> vertxInit = config.getVertxInit();
    if (vertxInit != null) {
      vertxInit.handle(vertx);
    }

    HttpServer httpServer = vertx.createHttpServer();
    Handler<HttpServer> httpServerInit = config.getHttpServerInit();
    if (httpServerInit != null) {
      httpServerInit.handle(httpServer);
    }

    return new RatpackApp(vertx, httpServer, config.getHost(), config.getPort(), router, templateRenderer, publicDir);
  }

}
