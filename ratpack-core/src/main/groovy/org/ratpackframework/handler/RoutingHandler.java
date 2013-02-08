package org.ratpackframework.handler;

import com.google.inject.Inject;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.Handler;
import org.ratpackframework.error.ErrorHandler;
import org.ratpackframework.handler.internal.DefaultHttpExchange;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.routing.RoutedHttpExchange;

import javax.inject.Named;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoutingHandler extends SimpleChannelUpstreamHandler {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final Handler<Routed<HttpExchange>> router;
  private final ErrorHandler errorHandler;

  @Inject
  public RoutingHandler(@Named("mainRouter") Handler<Routed<HttpExchange>> router, ErrorHandler errorHandler) {
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
    final HttpExchange exchange = new DefaultHttpExchange(null, request, response, ctx, errorHandler);

    try {
      RoutedHttpExchange routedHttpExchange = new RoutedHttpExchange(exchange, new Handler<Routed<HttpExchange>>() {
        @Override
        public void handle(Routed<HttpExchange> event) {
          exchange.end(HttpResponseStatus.NOT_FOUND);
        }
      });
      router.handle(routedHttpExchange);
    } catch (Exception e) {
      exchange.error(e);
    }
  }

}
