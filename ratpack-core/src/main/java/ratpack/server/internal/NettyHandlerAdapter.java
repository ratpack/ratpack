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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.error.internal.DefaultProductionErrorHandler;
import ratpack.error.internal.ErrorHandler;
import ratpack.event.internal.DefaultEventController;
import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;
import ratpack.file.FileRenderer;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.ActivationBackedMimeTypes;
import ratpack.file.internal.DefaultFileRenderer;
import ratpack.file.internal.ShouldCompressPredicate;
import ratpack.form.internal.FormParser;
import ratpack.func.Action;
import ratpack.func.Pair;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.handling.Redirector;
import ratpack.handling.RequestOutcome;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.handling.direct.internal.DefaultDirectChannelAccess;
import ratpack.handling.internal.*;
import ratpack.http.MutableHeaders;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClients;
import ratpack.http.internal.*;
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
import ratpack.sse.internal.ServerSentEventsRenderer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME;
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final AttributeKey<DefaultResponseTransmitter> RESPONSE_TRANSMITTER_ATTRIBUTE_KEY = AttributeKey.valueOf(DefaultResponseTransmitter.class.getName());

  private final static Logger LOGGER = LoggerFactory.getLogger(NettyHandlerAdapter.class);

  private final Handler[] handlers;

  private final ConcurrentHashMap<Channel, Action<Object>> channelSubscriptions = new ConcurrentHashMap<>(0);

  private final DefaultContext.ApplicationConstants applicationConstants;
  private final ExecController execController;
  private final LaunchConfig launchConfig;
  private final Predicate<Pair<Long, String>> shouldCompress;

  private Registry registry;

  private final boolean addResponseTimeHeader;
  private final ExecControl execControl;

  public NettyHandlerAdapter(Stopper stopper, Handler handler, LaunchConfig launchConfig) {
    this.handlers = ChainHandler.unpack(handler);
    this.launchConfig = launchConfig;
    this.registry = buildBaseRegistry(stopper, launchConfig);
    this.addResponseTimeHeader = launchConfig.isTimeResponses();
    this.applicationConstants = new DefaultContext.ApplicationConstants(launchConfig, new DefaultRenderController(), Handlers.notFound());
    this.execController = launchConfig.getExecController();
    this.execControl = execController.getControl();

    if (launchConfig.isCompressResponses()) {
      ImmutableSet<String> blacklist = launchConfig.getCompressionMimeTypeBlackList();
      this.shouldCompress = new ShouldCompressPredicate(
        launchConfig.getCompressionMinSize(),
        launchConfig.getCompressionMimeTypeWhiteList(),
        blacklist.isEmpty() ? ActivationBackedMimeTypes.getDefaultExcludedMimeTypes() : blacklist
      );
    } else {
      this.shouldCompress = Predicates.alwaysFalse();
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
    if (!(msg instanceof FullHttpRequest)) {
      Action<Object> subscriber = channelSubscriptions.get(channelHandlerContext.channel());
      if (subscriber != null) {
        subscriber.execute(msg);
        return;
      }
    }
    super.channelRead(channelHandlerContext, msg);
  }

  public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest nettyRequest) throws Exception {
    if (!nettyRequest.getDecoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    final long startTime = addResponseTimeHeader ? System.nanoTime() : 0;
    final Request request = new DefaultRequest(new NettyHeadersBackedHeaders(nettyRequest.headers()), nettyRequest.getMethod().name(), nettyRequest.getUri(), nettyRequest.content());
    final Channel channel = ctx.channel();
    final HttpHeaders nettyHeaders = new DefaultHttpHeaders(false);
    final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyHeaders);
    final DefaultEventController<RequestOutcome> requestOutcomeEventController = new DefaultEventController<>();
    final AtomicBoolean transmitted = new AtomicBoolean(false);

    final DefaultResponseTransmitter responseTransmitter = new DefaultResponseTransmitter(transmitted, execControl, channel, nettyRequest, request, nettyHeaders, requestOutcomeEventController, launchConfig.isCompressResponses(), shouldCompress, startTime);

    final Response response = new DefaultResponse(execControl, responseHeaders, ctx.alloc(), responseTransmitter);
    ctx.attr(RESPONSE_TRANSMITTER_ATTRIBUTE_KEY).set(responseTransmitter);

    InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();
    final BindAddress bindAddress = new InetSocketAddressBackedBindAddress(socketAddress);

    Action<Action<Object>> subscribeHandler = thing -> {
      transmitted.set(true);
      channelSubscriptions.put(channel, thing);
      channel.closeFuture().addListener(future -> channelSubscriptions.remove(channel));
    };

    final DirectChannelAccess directChannelAccess = new DefaultDirectChannelAccess(channel, subscribeHandler);

    final DefaultContext.RequestConstants requestConstants = new DefaultContext.RequestConstants(
      applicationConstants, bindAddress, request, response, directChannelAccess, requestOutcomeEventController.getRegistry()
    );

    DefaultContext.start(execController.getControl(), requestConstants, registry, handlers, execution -> {
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

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    ctx.attr(RESPONSE_TRANSMITTER_ATTRIBUTE_KEY).get().writabilityChanged();
  }

  private boolean isIgnorableException(Throwable throwable) {
    // There really does not seem to be a better way of detecting this kind of exception
    return throwable instanceof IOException && throwable.getMessage().equals("Connection reset by peer");
  }

  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderConstants.CONTENT_TYPE, HttpHeaderConstants.PLAIN_TEXT_UTF8);

    // Close the connection as soon as the error message is sent.
    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }

  public static Registry buildBaseRegistry(Stopper stopper, LaunchConfig launchConfig) {
    ErrorHandler errorHandler = launchConfig.isDevelopment() ? new DefaultDevelopmentErrorHandler() : new DefaultProductionErrorHandler();

    RegistryBuilder registryBuilder = Registries.registry()
      .add(Stopper.class, stopper)
      .add(MimeTypes.class, new ActivationBackedMimeTypes())
      .add(PublicAddress.class, new DefaultPublicAddress(launchConfig.getPublicAddress(), launchConfig.getSSLContext() == null ? HTTP_SCHEME : HTTPS_SCHEME))
      .add(Redirector.class, new DefaultRedirector())
      .add(ClientErrorHandler.class, errorHandler)
      .add(ServerErrorHandler.class, errorHandler)
      .add(LaunchConfig.class, launchConfig)
      .add(FileRenderer.class, new DefaultFileRenderer())
      .add(ServerSentEventsRenderer.TYPE, new ServerSentEventsRenderer(launchConfig.getBufferAllocator()))
      .add(HttpResponseChunksRenderer.TYPE, new HttpResponseChunksRenderer())
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
