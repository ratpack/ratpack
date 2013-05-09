package org.ratpackframework.http.internal;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.ratpackframework.context.DefaultContext;
import org.ratpackframework.error.internal.ErrorHandler;
import org.ratpackframework.error.internal.TopLevelErrorHandlingContext;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.routing.internal.DefaultExchange;

public class NettyRoutingAdapter extends ChannelInboundMessageHandlerAdapter<FullHttpRequest> {

  private final Handler handler;

  public NettyRoutingAdapter(Handler handler) {
    this.handler = handler;
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) throws Exception {
    if (!nettyRequest.getDecoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    Request request = new DefaultRequest(nettyRequest);
    Response response = new DefaultResponse(nettyResponse, ctx.channel());

    final Exchange exchange = new DefaultExchange(request, response, ctx, new DefaultContext(), new Handler() {
      @Override
      public void handle(Exchange exchange) {
        exchange.getResponse().status(404).send();
      }
    });

    exchange.nextWithContext(new TopLevelErrorHandlingContext(), new ErrorHandler(handler));
  }

  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    if (ctx.channel().isActive()) {
      sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }
  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Close the connection as soon as the error message is sent.
    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }
}
