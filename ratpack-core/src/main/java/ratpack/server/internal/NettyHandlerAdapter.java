/*
 * Copyright 2013 the original author or authors.
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

package ratpack.server.internal;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.error.internal.DefaultClientErrorHandler;
import ratpack.error.internal.DefaultServerErrorHandler;
import ratpack.event.internal.DefaultEventController;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.file.FileRenderer;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.*;
import ratpack.form.internal.FormParser;
import ratpack.func.Action;
import ratpack.func.Actions;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.Redirector;
import ratpack.handling.RequestOutcome;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.handling.direct.internal.DefaultDirectChannelAccess;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.internal.DefaultRedirector;
import ratpack.handling.internal.DescribingHandler;
import ratpack.handling.internal.DescribingHandlers;
import ratpack.http.MutableHeaders;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClients;
import ratpack.http.internal.*;
import ratpack.http.stream.HttpResponseChunksRenderer;
import ratpack.http.stream.internal.DefaultHttpResponseChunksRenderer;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.CharSequenceRenderer;
import ratpack.render.internal.DefaultCharSequenceRenderer;
import ratpack.render.internal.DefaultRenderController;
import ratpack.server.BindAddress;
import ratpack.server.PublicAddress;
import ratpack.server.Stopper;
import ratpack.stream.ServerSentEventsRenderer;
import ratpack.stream.internal.DefaultServerSentEventsRenderer;
import ratpack.stream.internal.DefaultStreamTransmitter;
import ratpack.stream.internal.StreamTransmitter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME;
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends SimpleChannelInboundHandler<FullHttpRequest> {

  private final static Logger LOGGER = LoggerFactory.getLogger(NettyHandlerAdapter.class);

  private final Handler[] handlers;
  private final Handler return404;

  private final ConcurrentHashMap<Channel, Action<Object>> channelSubscriptions = new ConcurrentHashMap<>(0);

  private final DefaultContext.ApplicationConstants applicationConstants;
  private final ExecController execController;
  private final LaunchConfig launchConfig;

  private Registry registry;

  private final boolean addResponseTimeHeader;
  private final boolean compressResponses;
  private final long compressionMinSize;
  private final ImmutableSet<String> compressionMimeTypeWhiteList;
  private final ImmutableSet<String> compressionMimeTypeBlackList;

  public NettyHandlerAdapter(Stopper stopper, Handler handler, LaunchConfig launchConfig) {
    this.handlers = new Handler[]{handler};
    this.return404 = Handlers.notFound();
    this.launchConfig = launchConfig;
    this.registry = buildBaseRegistry(stopper, launchConfig);
    this.addResponseTimeHeader = launchConfig.isTimeResponses();
    this.compressResponses = launchConfig.isCompressResponses();
    this.compressionMinSize = launchConfig.getCompressionMinSize();
    this.compressionMimeTypeWhiteList = launchConfig.getCompressionMimeTypeWhiteList();
    this.compressionMimeTypeBlackList = launchConfig.getCompressionMimeTypeBlackList();
    this.applicationConstants = new DefaultContext.ApplicationConstants(launchConfig, new DefaultRenderController());
    this.execController = launchConfig.getExecController();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof FullHttpRequest)) {
      Action<Object> subscriber = channelSubscriptions.get(ctx.channel());
      if (subscriber != null) {
        subscriber.execute(msg);
        return;
      }
    }
    super.channelRead(ctx, msg);
  }

  public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest nettyRequest) throws Exception {
    if (!nettyRequest.getDecoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    final long startTime = addResponseTimeHeader ? System.nanoTime() : 0;
    final Request request = new DefaultRequest(new NettyHeadersBackedHeaders(nettyRequest.headers()), nettyRequest.getMethod().name(), nettyRequest.getUri(), nettyRequest.content());
    final Channel channel = ctx.channel();
    final DefaultMutableStatus responseStatus = new DefaultMutableStatus();
    final HttpHeaders nettyHeaders = new DefaultHttpHeaders(false);
    final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyHeaders);
    final MimeTypes mimeTypes = registry.get(MimeTypes.class);
    final DefaultEventController<RequestOutcome> requestOutcomeEventController = new DefaultEventController<>();
    final AtomicBoolean transmitted = new AtomicBoolean(false);

    final ResponseTransmitter responseTransmitter = new DefaultResponseTransmitter(transmitted, channel, nettyRequest, request, nettyHeaders, responseStatus, requestOutcomeEventController, startTime);
    final Action<Action<? super ResponseTransmitter>> responseTransmitterWrapper = Actions.actionAction(responseTransmitter);

    final FileHttpTransmitter fileHttpTransmitter = new DefaultFileHttpTransmitter(nettyHeaders, mimeTypes,
      compressResponses, compressionMinSize, compressionMimeTypeWhiteList, compressionMimeTypeBlackList, responseTransmitterWrapper);
    StreamTransmitter streamTransmitter = new DefaultStreamTransmitter(responseTransmitter);

    final Response response = new DefaultResponse(responseStatus, responseHeaders, fileHttpTransmitter, streamTransmitter, ctx.alloc(), new Action<ByteBuf>() {
      @Override
      public void execute(final ByteBuf byteBuf) throws Exception {
        responseTransmitterWrapper.execute(new Action<ResponseTransmitter>() {
          @Override
          public void execute(ResponseTransmitter responseTransmitter) throws Exception {
            nettyHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, byteBuf.writerIndex());
            responseTransmitter.transmit(new DefaultHttpContent(byteBuf));
          }
        });
      }
    });

    InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();
    final BindAddress bindAddress = new InetSocketAddressBackedBindAddress(socketAddress);

    Action<Action<Object>> subscribeHandler = new Action<Action<Object>>() {
      @Override
      public void execute(Action<Object> thing) throws Exception {
        transmitted.set(true);
        channelSubscriptions.put(channel, thing);
        channel.closeFuture().addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            channelSubscriptions.remove(channel);
          }
        });
      }
    };

    final DirectChannelAccess directChannelAccess = new DefaultDirectChannelAccess(channel, subscribeHandler);

    final DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(
      applicationConstants, bindAddress, request, response, directChannelAccess, requestOutcomeEventController.getRegistry()
    );

    DefaultContext.start(execController.getControl(), requestConstants, registry, handlers, return404, new Action<Execution>() {
      @Override
      public void execute(Execution execution) throws Exception {
        if (!transmitted.get()) {
          Handler lastHandler = requestConstants.handler;
          StringBuilder description = new StringBuilder();
          description
            .append("No response sent for ")
            .append(request.getMethod().getName())
            .append(" request to ")
            .append(request.getUri())
            .append(" (last handler: ");

          if (lastHandler instanceof DescribingHandler) {
            ((DescribingHandler) lastHandler).describeTo(description);
          } else {
            DescribingHandlers.describeTo(lastHandler, description);
          }

          description.append(")");
          String message = description.toString();
          LOGGER.warn(message);

          response.status(500);

          if (launchConfig.isDevelopment()) {
            response.send(message);
          } else {
            response.send();
          }
        }
      }
    });
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (!isIgnorableException(cause)) {
      LOGGER.error("", cause);
      if (ctx.channel().isActive()) {
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  private boolean isIgnorableException(Throwable throwable) {
    // There really does not seem to be a better way of detecting this kind of exception
    return throwable instanceof IOException && throwable.getMessage().equals("Connection reset by peer");
  }

  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderConstants.CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Close the connection as soon as the error message is sent.
    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }

  public static Registry buildBaseRegistry(Stopper stopper, LaunchConfig launchConfig) {
    RegistryBuilder registryBuilder = Registries.registry()
      .add(Stopper.class, stopper)
      .add(MimeTypes.class, new ActivationBackedMimeTypes())
      .add(PublicAddress.class, new DefaultPublicAddress(launchConfig.getPublicAddress(), launchConfig.getSSLContext() == null ? HTTP_SCHEME : HTTPS_SCHEME))
      .add(Redirector.class, new DefaultRedirector())
      .add(ClientErrorHandler.class, new DefaultClientErrorHandler())
      .add(ServerErrorHandler.class, new DefaultServerErrorHandler())
      .add(LaunchConfig.class, launchConfig)
      .add(FileRenderer.class, new DefaultFileRenderer())
      .add(ServerSentEventsRenderer.class, new DefaultServerSentEventsRenderer())
      .add(HttpResponseChunksRenderer.class, new DefaultHttpResponseChunksRenderer())
      .add(CharSequenceRenderer.class, new DefaultCharSequenceRenderer())
      .add(FormParser.class, FormParser.multiPart())
      .add(FormParser.class, FormParser.urlEncoded())
      .add(HttpClient.class, HttpClients.httpClient(launchConfig));

    if (launchConfig.isHasBaseDir()) {
      registryBuilder.add(FileSystemBinding.class, launchConfig.getBaseDir());
    }

    return registryBuilder.build();
  }

}
