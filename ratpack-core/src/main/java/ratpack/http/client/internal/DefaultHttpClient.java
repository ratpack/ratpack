/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Status;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.URI;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultHttpClient implements HttpClient {

  private final ExecController execController;
  private final ByteBufAllocator byteBufAllocator;
  private final int maxContentLengthBytes;

  public DefaultHttpClient(ExecController execController, ByteBufAllocator byteBufAllocator, int maxContentLengthBytes) {
    this.execController = execController;
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLengthBytes = maxContentLengthBytes;
  }

  @Override
  public Promise<ReceivedResponse> get(Action<? super RequestSpec> requestConfigurer) {
    return request(requestConfigurer);
  }

  private static class Post implements Action<RequestSpec> {
    @Override
    public void execute(RequestSpec requestSpec) throws Exception {
      requestSpec.method("POST");
    }
  }

  public Promise<ReceivedResponse> post(Action<? super RequestSpec> action) {
    return request(Action.join(new Post(), action));
  }

  @Override
  public Promise<ReceivedResponse> request(final Action<? super RequestSpec> requestConfigurer) {
    final ExecControl execControl = execController.getControl();
    final Execution execution = execControl.getExecution();
    final EventLoopGroup eventLoopGroup = execController.getEventLoopGroup();

    try {
      RequestAction requestAction = new RequestAction(requestConfigurer, execution, eventLoopGroup, byteBufAllocator, maxContentLengthBytes);
      return execController.getControl().promise(requestAction);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  private static ByteBuf initBufferReleaseOnExecutionClose(final ByteBuf responseBuffer, Execution execution) {
    execution.onCleanup(responseBuffer::release);
    return responseBuffer.retain();
  }

  private static String getFullPath(URI uri) {
    StringBuilder sb = new StringBuilder(uri.getRawPath());
    String query = uri.getRawQuery();
    if (query != null) {
      sb.append("?").append(query);
    }

    return sb.toString();
  }

  private static class RequestAction implements Action<Fulfiller<ReceivedResponse>> {

    final Execution execution;
    final EventLoopGroup eventLoopGroup;
    final Action<? super RequestSpec> requestConfigurer;
    final boolean finalUseSsl;
    final String host;
    final int port;
    final MutableHeaders headers;
    final RequestSpecBacking requestSpecBacking;
    final URI uri;
    private final ByteBufAllocator byteBufAllocator;
    private final int maxContentLengthBytes;

    public RequestAction(Action<? super RequestSpec> requestConfigurer, Execution execution, EventLoopGroup eventLoopGroup, ByteBufAllocator byteBufAllocator, int maxContentLengthBytes) {
      this.execution = execution;
      this.eventLoopGroup = eventLoopGroup;
      this.requestConfigurer = requestConfigurer;
      this.byteBufAllocator = byteBufAllocator;
      this.maxContentLengthBytes = maxContentLengthBytes;

      headers = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
      requestSpecBacking = new RequestSpecBacking(headers, byteBufAllocator);

      try {
        requestConfigurer.execute(requestSpecBacking.asSpec());
      } catch (Exception e) {
        throw uncheck(e);
      }

      uri = requestSpecBacking.getUrl();

      String scheme = uri.getScheme();
      boolean useSsl = false;
      if (scheme.equals("https")) {
        useSsl = true;
      } else if (!scheme.equals("http")) {
        throw new IllegalArgumentException(String.format("URL '%s' is not a http url", uri.toString()));
      }
      finalUseSsl = useSsl;


      host = uri.getHost();
      port = uri.getPort() < 0 ? (useSsl ? 443 : 80) : uri.getPort();

    }


    public void execute(final Fulfiller<ReceivedResponse> fulfiller) throws Exception {
      final Bootstrap b = new Bootstrap();
      b.group(eventLoopGroup)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();

            if (finalUseSsl) {
              SSLEngine engine = SSLContext.getDefault().createSSLEngine();
              engine.setUseClientMode(true);
              p.addLast("ssl", new SslHandler(engine));
            }

            p.addLast("codec", new HttpClientCodec());
            p.addLast("aggregator", new HttpObjectAggregator(maxContentLengthBytes));
            p.addLast("handler", new SimpleChannelInboundHandler<HttpObject>() {
              @Override
              public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                if (msg instanceof FullHttpResponse) {
                  final FullHttpResponse response = (FullHttpResponse) msg;
                  final Headers headers = new NettyHeadersBackedHeaders(response.headers());
                  String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE.toString());
                  ByteBuf responseBuffer = initBufferReleaseOnExecutionClose(response.content(), execution);
                  final ByteBufBackedTypedData typedData = new ByteBufBackedTypedData(responseBuffer, DefaultMediaType.get(contentType));

                  final Status status = new DefaultStatus(response.getStatus());


                  int maxRedirects = requestSpecBacking.getMaxRedirects();
                  String locationValue = headers.get("Location");

                  URI locationUrl = null;
                  if (locationValue != null) {
                    locationUrl = new URI(locationValue);
                  }

                  //Check for redirect and location header if it is follow redirect if we have request forwarding left
                  if (isRedirect(status) && maxRedirects > 0 && locationUrl != null) {
                    Action<? super RequestSpec> redirectRequestConfg = Action.join(requestConfigurer, new RedirectConfigurer(locationUrl, maxRedirects - 1));
                    RequestAction requestAction = new RequestAction(redirectRequestConfg, execution, eventLoopGroup, byteBufAllocator, maxContentLengthBytes);
                    requestAction.execute(fulfiller);
                  } else {
                    //Just fulfill what ever we currently have
                    fulfiller.success(new DefaultReceivedResponse(status, headers, typedData));
                  }
                }
              }

              @Override
              public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                ctx.close();
                fulfiller.error(cause);
              }
            });
          }
        });

      ChannelFuture connectFuture = b.connect(host, port);
      connectFuture.addListener(f1 -> {
        if (connectFuture.isSuccess()) {

          String fullPath = getFullPath(uri);
          FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(requestSpecBacking.getMethod()), fullPath, requestSpecBacking.getBody());
          if (headers.get(HttpHeaderConstants.HOST) == null) {
            headers.set(HttpHeaderConstants.HOST, host);
          }
          headers.set(HttpHeaderConstants.CONNECTION, HttpHeaders.Values.CLOSE);
          int contentLength = request.content().readableBytes();
          if (contentLength > 0) {
            headers.set(HttpHeaderConstants.CONTENT_LENGTH, Integer.toString(contentLength, 10));
          }

          HttpHeaders requestHeaders = request.headers();

          for (String name : headers.getNames()) {
            requestHeaders.set(name, headers.getAll(name));
          }

          ChannelFuture writeFuture = connectFuture.channel().writeAndFlush(request);
          writeFuture.addListener(f2 -> {
            if (!writeFuture.isSuccess()) {
              writeFuture.channel().close();
              fulfiller.error(writeFuture.cause());
            }
          });
        } else {
          connectFuture.channel().close();
          fulfiller.error(connectFuture.cause());
        }
      });
    }


    private static boolean isRedirect(Status status) {
      int code = status.getCode();
      return code >= 300 && code < 400;
    }
  }

  private static class RedirectConfigurer implements Action<RequestSpec> {

    private URI url;
    private int maxRedirect;

    RedirectConfigurer(URI url, int maxRedirect) {
      this.url = url;
      this.maxRedirect = maxRedirect;
    }

    @Override
    public void execute(RequestSpec requestSpec) throws Exception {
      requestSpec.url(httpUrlSpec -> httpUrlSpec.set(url));
      requestSpec.redirects(maxRedirect);
    }
  }

}
