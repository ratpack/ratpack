package org.ratpackframework.handler;

import com.google.inject.Inject;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.error.ErroredHttpExchange;
import org.ratpackframework.http.internal.DefaultHttpExchange;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;
import org.ratpackframework.routing.RoutedHttpExchange;

import javax.inject.Named;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.ratpackframework.bootstrap.internal.RootModule.MAIN_HTTP_ERROR_HANDLER;
import static org.ratpackframework.bootstrap.internal.RootModule.MAIN_HTTP_HANDLER;

public class NettyRoutingAdapter extends SimpleChannelUpstreamHandler {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final Handler<Routed<HttpExchange>> router;
  private final Handler<ErroredHttpExchange> errorHandler;

  @Inject
  public NettyRoutingAdapter(
      @Named(MAIN_HTTP_HANDLER) Handler<Routed<HttpExchange>> router,
      @Named(MAIN_HTTP_ERROR_HANDLER) Handler<ErroredHttpExchange> errorHandler) {
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
