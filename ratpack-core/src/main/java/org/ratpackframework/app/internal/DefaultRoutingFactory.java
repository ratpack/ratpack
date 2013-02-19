package org.ratpackframework.app.internal;

import com.google.inject.Injector;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.app.Routing;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import javax.inject.Inject;
import java.util.List;

public class DefaultRoutingFactory implements RoutingFactory {

  private final Injector injector;
  private final ResponseFactory responseFactory;
  private final RequestScope requestScope;

  @Inject
  public DefaultRoutingFactory(Injector injector, ResponseFactory responseFactory, RequestScope requestScope) {
    this.injector = injector;
    this.responseFactory = responseFactory;
    this.requestScope = requestScope;
  }

  @Override
  public Routing create(List<Handler<Routed<HttpExchange>>> routes) {
    return new RoutingBuilder(injector, requestScope, routes, responseFactory);
  }

}
