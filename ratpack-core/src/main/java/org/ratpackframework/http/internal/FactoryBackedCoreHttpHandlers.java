package org.ratpackframework.http.internal;

import org.ratpackframework.Factory;
import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.Handler;
import org.ratpackframework.http.CoreHttpHandlers;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

public class FactoryBackedCoreHttpHandlers implements CoreHttpHandlers {

  private final Factory<CoreHttpHandlers> factory;

  public FactoryBackedCoreHttpHandlers(Factory<CoreHttpHandlers> factory) {
    this.factory = factory;
  }

  @Override
  public Handler<Routed<HttpExchange>> getAppHandler() {
    return factory.create().getAppHandler();
  }

  @Override
  public Handler<ErroredHttpExchange> getErrorHandler() {
    return factory.create().getErrorHandler();
  }

  @Override
  public Handler<Routed<HttpExchange>> getNotFoundHandler() {
    return factory.create().getNotFoundHandler();
  }

}
