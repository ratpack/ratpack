package org.ratpackframework.http.internal;

import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

import java.util.Collection;
import java.util.List;

import static org.ratpackframework.util.CollectionUtils.toUpperCase;

public class MethodHandler implements Handler {

  private final List<String> methods;
  private final Handler delegate;

  public MethodHandler(Collection<String> methods, Handler delegate) {
    this.methods = toUpperCase(methods);
    this.delegate = delegate;
  }

  public MethodHandler(List<String> methods, Handler delegate) {
    this.methods = toUpperCase(methods);
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    String methodName = exchange.getRequest().getMethod().getName();
    if (methods.contains(methodName)) {
      exchange.next(delegate);
    } else {
      exchange.getResponse().status(415).send();
    }
  }
}
