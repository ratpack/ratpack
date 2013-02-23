package org.ratpackframework.app.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.app.Endpoint;
import org.ratpackframework.app.Request;
import org.ratpackframework.app.Response;

public class InjectingEndpoint implements Endpoint {
  private final Class<? extends Endpoint> endpointType;
  private final Injector injector;

  public InjectingEndpoint(final Class<? extends Endpoint> endpointType, Injector parentInjector) {
    this.endpointType = endpointType;
    this.injector = parentInjector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(endpointType);
      }
    });
  }

  @Override
  public void respond(Request request, Response response) {
    injector.getInstance(endpointType).respond(request, response);
  }

}
