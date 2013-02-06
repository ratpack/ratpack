package org.ratpackframework.handler;

import com.google.inject.Injector;
import org.ratpackframework.routing.FinalizedResponse;
import org.ratpackframework.routing.RoutedRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

public class RoutingHandler implements Handler<HttpServerRequest> {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Handler<RoutedRequest> router;
  private final ErrorHandler errorHandler;
  private final Handler<HttpServerRequest> notFoundHandler;

  public RoutingHandler(Handler<RoutedRequest> router, ErrorHandler errorHandler, Handler<HttpServerRequest> notFoundHandler) {
    this.router = router;
    this.errorHandler = errorHandler;
    this.notFoundHandler = notFoundHandler;
  }

  @Override
  public void handle(final HttpServerRequest request) {
    request.pause();

    if (logger.isInfoEnabled()) {
      logger.info("received " + request.uri);
    }

    RoutedRequest routedRequest = new RoutedRequest(request, errorHandler, notFoundHandler, errorHandler.asyncHandler(request, new Handler<FinalizedResponse>() {
      @Override
      public void handle(FinalizedResponse response) {
        HttpServerResponse realResponse = request.response;

        realResponse.statusCode = response.getStatus();

        for (Map.Entry<String, Object> entry : response.getHeaders().entrySet()) {
          Object value = entry.getValue();
          @SuppressWarnings("unchecked")
          Iterable<Object> values = value instanceof Iterable ? (Iterable<Object>) value : Arrays.asList(value);
          for (Object singleValue : values) {
            realResponse.putHeader(entry.getKey(), singleValue);
          }
        }
        Buffer buffer = response.getBuffer();
        if (buffer == null) {
          realResponse.end();
        } else {
          realResponse.end(buffer);
        }
      }
    }));

    router.handle(routedRequest);
  }

}
