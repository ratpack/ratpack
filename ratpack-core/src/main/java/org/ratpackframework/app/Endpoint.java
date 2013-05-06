package org.ratpackframework.app;

import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;

public interface Endpoint {

  void respond(Request request, Response response);

}
