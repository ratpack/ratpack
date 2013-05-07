package org.ratpackframework.http.internal;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.*;
import org.ratpackframework.context.DefaultContext;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.routing.internal.DefaultExchange;

public class NettyRoutingAdapter extends SimpleChannelUpstreamHandler {

  private final Handler handler;

  public NettyRoutingAdapter(Handler handler) {
    this.handler = handler;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
    HttpRequest nettyRequest = (HttpRequest) event.getMessage();
    HttpResponse nettyResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    Request request = new DefaultRequest(nettyRequest);
    Response response = new DefaultResponse(nettyResponse, ctx.getChannel());

    final Exchange exchange = new DefaultExchange(request, response, ctx, new DefaultContext(), new Handler() {
      @Override
      public void handle(Exchange exchange) {
        exchange.getResponse().status(404).send();
      }
    });

    try {
      handler.handle(exchange);
    } catch (Exception e) {
      System.err.println("UNHANDLED EXCEPTION: " + request.getUri());
      e.printStackTrace(System.err);
      exchange.getResponse().status(500).send();
    }
  }

}
