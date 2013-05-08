package org.ratpackframework.groovy

import org.ratpackframework.Action
import org.ratpackframework.routing.Exchange
import org.ratpackframework.routing.Handler
import org.ratpackframework.routing.Handlers
import org.ratpackframework.routing.Routing

public abstract class ClosureHandlers {

  public static Handler handler(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> handler) {
    new Handler() {
      @Override
      public void handle(Exchange exchange) {
        Closures.configure(exchange, handler);
      }
    }
  }

  public static Handler context(final Object context, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> routes) {
    Handlers.context(context, routingAction(routes));
  }

  private static Action<Routing> routingAction(@DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure<?> routes) {
    Closures.action(Routing.class, routes)
  }

  public static Handler fsContext(String path, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.fsContext(path, routingAction(routing))
  }

  public static Handler path(String path, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.path(path, routingAction(routing))
  }

  public static Handler exactPath(String path, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.exactPath(path, routingAction(routing))
  }

  public static Handler method(Collection<String> methods, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.method(methods, routingAction(routing))
  }

  public static Handler handler(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    Handlers.path(path, handler(closure))
  }

  public static Handler handler(String path, String method, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, Arrays.asList(method), closure)
  }

  public static Handler handler(String path, Collection<String> methods, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    Handlers.exactPath(path, Handlers.method(methods, handler(closure)))
  }

  public static Handler get(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "get", closure)
  }

  public static Handler post(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "post", closure)
  }

  public static Handler put(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "put", closure)
  }

  public static Handler delete(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "delete", closure)
  }

}
