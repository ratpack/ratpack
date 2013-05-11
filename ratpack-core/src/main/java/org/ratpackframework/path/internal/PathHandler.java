package org.ratpackframework.path.internal;

import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.path.PathContext;

public class PathHandler implements Handler {

  private final PathBinding binding;
  private final Handler delegate;

  public PathHandler(PathBinding binding, Handler delegate) {
    this.binding = binding;
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    PathContext childContext = binding.bind(exchange.getRequest().getPath(), exchange.getContext().maybeGet(PathContext.class));
    if (childContext != null) {
      exchange.nextWithContext(childContext, delegate);
    } else {
      exchange.next();
    }
  }
}
