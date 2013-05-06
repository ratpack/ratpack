package org.ratpackframework.path.internal;

import org.ratpackframework.http.Exchange;
import org.ratpackframework.http.Handler;
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
    PathContext childContext = binding.bind(exchange.getRequest().getPath(), exchange.getContext().get(PathContext.class));
    if (childContext != null) {
      exchange.nextWithContext(childContext, delegate);
    } else {
      exchange.next();
    }
  }
}
