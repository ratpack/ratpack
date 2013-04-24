package org.ratpackframework.http.internal;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.Action;
import org.ratpackframework.http.CoreHttpHandlers;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.routing.RoutedHttpExchange;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NettyRoutingAdapter extends SimpleChannelUpstreamHandler {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final CoreHttpHandlers coreHttpHandlers;

  public NettyRoutingAdapter(CoreHttpHandlers coreHttpHandlers) {
    this.coreHttpHandlers = coreHttpHandlers;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    HttpRequest request = (HttpRequest) event.getMessage();
    if (logger.isLoggable(Level.INFO)) {
      logger.info("received " + request.getUri());
    }

    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    final HttpExchange exchange = new DefaultHttpExchange(null, request, response, ctx, coreHttpHandlers.getErrorHandler());

    try {
      RoutedHttpExchange routedHttpExchange = new RoutedHttpExchange(exchange, new Action<Routed<HttpExchange>>() {
        @Override
        public void execute(Routed<HttpExchange> event) {
          exchange.end(HttpResponseStatus.NOT_FOUND);
        }
      });
      coreHttpHandlers.getAppHandler().execute(routedHttpExchange);
    } catch (Exception e) {
      exchange.error(e);
    }
  }

}
