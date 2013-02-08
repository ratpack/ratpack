package org.ratpackframework.handler;

import com.google.inject.Inject;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.handler.internal.DefaultHttpExchange;
import org.ratpackframework.routing.RoutedRequest;

import javax.inject.Named;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoutingHandler extends SimpleChannelUpstreamHandler {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final Handler<RoutedRequest> router;
  private final ErrorHandler errorHandler;

  @Inject
  public RoutingHandler(@Named("mainRouter") Handler<RoutedRequest> router, ErrorHandler errorHandler) {
    this.router = router;
    this.errorHandler = errorHandler;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    HttpRequest request = (HttpRequest) event.getMessage();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("received " + request.getUri());
    }

    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    final HttpExchange exchange = new DefaultHttpExchange(request, response, ctx, errorHandler);

    try {
      RoutedRequest routedRequest = new RoutedRequest(exchange, new Handler<RoutedRequest>() {
        @Override
        public void handle(RoutedRequest event) {
          exchange.end(HttpResponseStatus.NOT_FOUND);
        }
      });
      router.handle(routedRequest);
    } catch (Exception e) {
      exchange.error(e);
    }
  }

}
