package org.ratpackframework.handler;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.Handler;

public class InjectingHandler<T> implements Handler<T> {
  private final Class<? extends Handler<T>> handlerType;
  private final Injector injector;

  public InjectingHandler(final Class<? extends Handler<T>> handlerType, Injector parentInjector) {
    this.handlerType = handlerType;
    this.injector = parentInjector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(handlerType);
      }
    });
  }

  @Override
  public void handle(T event) {
    injector.getInstance(handlerType).handle(event);
  }
}
