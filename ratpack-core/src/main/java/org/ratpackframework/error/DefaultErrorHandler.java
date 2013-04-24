package org.ratpackframework.error;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.Action;
import org.ratpackframework.http.HttpExchange;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Error handler that simply sends back a HTTP 500.
 */
public class DefaultErrorHandler implements Action<ErroredHttpExchange> {

  private final Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public void execute(ErroredHttpExchange erroredRequest) {
    HttpExchange exchange = erroredRequest.getExchange();
    Exception exception = erroredRequest.getException();
    logger.log(Level.WARNING, "error handling " + exchange.getRequest().getUri(), exception);
    exchange.end(HttpResponseStatus.INTERNAL_SERVER_ERROR);
  }
}
