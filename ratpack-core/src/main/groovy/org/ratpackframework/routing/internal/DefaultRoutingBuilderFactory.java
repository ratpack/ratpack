package org.ratpackframework.routing.internal;

import com.google.inject.Injector;
import org.ratpackframework.Handler;
import org.ratpackframework.Routing;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.inject.RequestScope;
import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.Routed;

import javax.inject.Inject;
import java.util.List;

public class DefaultRoutingBuilderFactory implements RoutingBuilderFactory {

  private final Injector injector;
  private final ResponseFactory responseFactory;
  private final RequestScope requestScope;

  @Inject
  public DefaultRoutingBuilderFactory(Injector injector, ResponseFactory responseFactory, RequestScope requestScope) {
    this.injector = injector;
    this.responseFactory = responseFactory;
    this.requestScope = requestScope;
  }

  @Override
  public Routing create(List<Handler<Routed<HttpExchange>>> routes) {
    return new RoutingBuilder(injector, requestScope, routes, responseFactory);
  }

}
