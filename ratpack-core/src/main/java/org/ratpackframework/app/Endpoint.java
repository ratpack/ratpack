package org.ratpackframework.app;

import org.ratpackframework.app.Request;
import org.ratpackframework.app.Response;

public interface Endpoint {

  void respond(Request request, Response response);

}
