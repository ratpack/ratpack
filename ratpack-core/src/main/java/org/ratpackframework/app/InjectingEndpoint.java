package org.ratpackframework.app;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.app.Endpoint;
import org.ratpackframework.app.Request;
import org.ratpackframework.app.Response;

/**
 * An endpoint that responds by creating a delegate endpoint instance via a given {@link Injector}.
 *
 * This facilitates dependency injection into endpoints and the use of {@link RequestScoped} services.
 * A new child module of the given {@code injector} is created internally and the given {@code endpointType} bound.
 * As this module is a child of the main app module, any services registered at initialisation are candidates for injection.
 * <p>
 * The {@link Request} and {@link Response} objects are also available for injection (as the same instances that will be passed
 * to {@link #respond(Request, Response)}) as the actual endpoint will be instantiated within the request scope.
 */
public class InjectingEndpoint implements Endpoint {
  private final Class<? extends Endpoint> endpointType;
  private final Injector injector;

  public InjectingEndpoint(Injector injector, Class<? extends Endpoint> endpointType) {
    this.endpointType = endpointType;
    this.injector = injector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(InjectingEndpoint.this.endpointType);
      }
    });
  }

  /**
   * Creates a new instance of the delegate endpoint type via the dependency injection engine and delegates to it.
   */
  @Override
  public void respond(Request request, Response response) {
    injector.getInstance(endpointType).respond(request, response);
  }

}
