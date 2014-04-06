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
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import ratpack.func.Action;
import ratpack.handling.ReadOnlyContext;
import ratpack.http.Headers;
import ratpack.http.TypedData;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestResult;
import ratpack.http.internal.ByteBufBackedTypedData;
import ratpack.http.internal.DefaultMediaType;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import ratpack.launch.LaunchConfig;
import ratpack.launch.internal.LaunchConfigInternal;
import ratpack.promise.Fulfiller;
import ratpack.promise.SuccessOrErrorPromise;

import java.net.URI;
import java.net.URISyntaxException;


public class DefaultHttpClient implements HttpClient {

  private final ReadOnlyContext context;

  public DefaultHttpClient(ReadOnlyContext context) {
    this.context = context;
  }

  @Override
  public SuccessOrErrorPromise<RequestResult> get(String httpUrl) {
    final URI uri;
    try {
      uri = new URI(httpUrl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }

    String scheme = uri.getScheme();
    if (!scheme.equals("http")) {
      throw new IllegalArgumentException("URL is not a http url");
    }

    final String host = uri.getHost();
    final int port = uri.getPort() < 0 ? 80 : uri.getPort();


    return context.promise(new Action<Fulfiller<RequestResult>>() {
      @Override
      public void execute(final Fulfiller<RequestResult> fulfiller) throws Exception {
        final Bootstrap b = new Bootstrap();
        b.group(getEventLoopGroup(context))
          .channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
              ChannelPipeline p = ch.pipeline();
              p.addLast("codec", new HttpClientCodec());
              p.addLast("aggregator", new HttpObjectAggregator(1048576));
              p.addLast("handler", new SimpleChannelInboundHandler<HttpObject>() {
                @Override
                public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                  if (msg instanceof FullHttpResponse) {
                    final FullHttpResponse response = (FullHttpResponse) msg;
                    final Headers headers = new NettyHeadersBackedHeaders(response.headers());
                    String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE.toString());
                    final ByteBufBackedTypedData typedData = new ByteBufBackedTypedData(response.content(), DefaultMediaType.get(contentType));

                    fulfiller.success(new RequestResult() {
                      @Override
                      public ReceivedResponse getResponse() {
                        return new ReceivedResponse() {
                          @Override
                          public Headers getHeaders() {
                            return headers;
                          }

                          @Override
                          public TypedData getBody() {
                            return typedData;
                          }
                        };
                      }
                    });
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

        b.connect(host, port).addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
              HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
              request.headers().set(HttpHeaders.Names.HOST, host);
              request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);

              future.channel().writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                  if (!future.isSuccess()) {
                    future.channel().close();
                    fulfiller.error(future.cause());
                  }
                }
              });
            } else {
              future.channel().close();
              fulfiller.error(future.cause());
            }
          }
        });
      }
    });
  }

  protected EventLoopGroup getEventLoopGroup(ReadOnlyContext context) {
    return ((LaunchConfigInternal) context.get(LaunchConfig.class)).getEventLoopGroup();
  }

}
