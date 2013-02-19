package org.ratpackframework.app.internal;

import org.ratpackframework.handler.Handler;
import org.ratpackframework.app.Request;
import org.ratpackframework.app.Response;

public class RequestScopeHandler implements Handler<Response> {

  private final RequestScope scope;
  private final Handler<Response> delegate;

  public RequestScopeHandler(RequestScope scope, Handler<Response> delegate) {
    this.scope = scope;
    this.delegate = delegate;
  }

  @Override
  public void handle(Response response) {
    scope.enter();
    try {
      Request request = response.getRequest();
      scope.seed(Request.class, request);
      scope.seed(Response.class, response);
      delegate.handle(response);
    } finally {
      scope.exit();
    }
  }
}
