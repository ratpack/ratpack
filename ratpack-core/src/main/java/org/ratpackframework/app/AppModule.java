package org.ratpackframework.app;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.servlet.RequestScoped;
import org.ratpackframework.app.internal.*;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.render.DefaultTextRenderer;
import org.ratpackframework.render.TextRenderer;
import org.ratpackframework.routing.NoopRouter;

import javax.inject.Singleton;

import static org.ratpackframework.bootstrap.RootModule.HTTP_HANDLER;
import static org.ratpackframework.bootstrap.RootModule.MAIN_APP_HTTP_HANDLER;

public class AppModule extends AbstractModule {


  @Override
  protected void configure() {
    bind(HTTP_HANDLER).annotatedWith(Names.named(MAIN_APP_HTTP_HANDLER)).toInstance(new NoopRouter<HttpExchange>());

    bind(ResponseFactory.class).to(DefaultResponseFactory.class).in(Singleton.class);
    bind(RoutingFactory.class).to(DefaultRoutingFactory.class);
    bind(RoutingConverter.class).to(DefaultRoutingConverter.class);

    bind(TextRenderer.class).to(DefaultTextRenderer.class);

    RequestScope requestScope = new RequestScope();
    bindScope(RequestScoped.class, requestScope);
    bind(RequestScope.class).toInstance(requestScope);

    bind(Request.class).toProvider(RequestScope.<Request>seededKeyProvider()).in(RequestScoped.class);
    bind(Response.class).toProvider(RequestScope.<Response>seededKeyProvider()).in(RequestScoped.class);
  }

}
