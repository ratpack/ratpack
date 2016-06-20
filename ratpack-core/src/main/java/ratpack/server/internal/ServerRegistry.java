/*
 * Copyright 2015 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.config.ConfigObject;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.error.internal.DefaultProductionErrorHandler;
import ratpack.error.internal.ErrorHandler;
import ratpack.exec.ExecController;
import ratpack.exec.ExecInitializer;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.internal.ExecControllerInternal;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.ActivationBackedMimeTypes;
import ratpack.file.internal.FileRenderer;
import ratpack.form.internal.FormParser;
import ratpack.func.Function;
import ratpack.handling.Redirector;
import ratpack.handling.RequestId;
import ratpack.handling.internal.UuidBasedRequestIdGenerator;
import ratpack.health.internal.HealthCheckResultsRenderer;
import ratpack.http.client.HttpClient;
import ratpack.impose.Impositions;
import ratpack.jackson.JsonRender;
import ratpack.jackson.internal.JsonParser;
import ratpack.jackson.internal.JsonRenderer;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.Renderable;
import ratpack.render.Renderer;
import ratpack.render.internal.CharSequenceRenderer;
import ratpack.render.internal.PromiseRenderer;
import ratpack.render.internal.PublisherRenderer;
import ratpack.render.internal.RenderableRenderer;
import ratpack.server.*;
import ratpack.sse.ServerSentEventStreamClient;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;

import static ratpack.util.Exceptions.uncheck;
import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME;
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME;

public abstract class ServerRegistry {
  public static Registry serverRegistry(RatpackServer ratpackServer, Impositions impositions, ExecControllerInternal execController, ServerConfig serverConfig, Function<? super Registry, ? extends Registry> userRegistryFactory) {
    Registry baseRegistry = buildBaseRegistry(ratpackServer, impositions, execController, serverConfig);
    Registry userRegistry = buildUserRegistry(userRegistryFactory, baseRegistry);

    execController.setInterceptors(ImmutableList.copyOf(userRegistry.getAll(ExecInterceptor.class)));
    execController.setInitializers(ImmutableList.copyOf(userRegistry.getAll(ExecInitializer.class)));

    return baseRegistry.join(userRegistry);
  }

  private static Registry buildUserRegistry(Function<? super Registry, ? extends Registry> userRegistryFactory, Registry baseRegistry) {
    Registry userRegistry;
    try {
      userRegistry = userRegistryFactory.apply(baseRegistry);
    } catch (Exception e) {
      Throwables.propagateIfPossible(e);
      throw new StartupFailureException("Failed to build user registry", e);
    }
    return userRegistry;
  }

  public static Registry buildBaseRegistry(RatpackServer ratpackServer, Impositions impositions, ExecController execController, ServerConfig serverConfig) {
    ErrorHandler errorHandler = serverConfig.isDevelopment() ? new DefaultDevelopmentErrorHandler() : new DefaultProductionErrorHandler();

    RegistryBuilder baseRegistryBuilder;
    try {
      PromiseRenderer promiseRenderer = new PromiseRenderer();
      PublisherRenderer publisherRenderer = new PublisherRenderer();

      baseRegistryBuilder = Registry.builder()
        .add(ServerConfig.class, serverConfig)
        .add(Impositions.class, impositions)
        .add(ByteBufAllocator.class, PooledByteBufAllocator.DEFAULT)
        .add(ExecController.class, execController)
        .add(MimeTypes.class, new ActivationBackedMimeTypes())
        .add(PublicAddress.class, Optional.ofNullable(serverConfig.getPublicAddress())
          .map(PublicAddress::of)
          .orElseGet(() -> PublicAddress.inferred(serverConfig.getSslContext() == null ? HTTP_SCHEME : HTTPS_SCHEME))
        )
        .add(Redirector.TYPE, Redirector.standard())
        .add(ClientErrorHandler.class, errorHandler)
        .add(ServerErrorHandler.class, errorHandler)
        .add(Renderer.typeOf(Path.class), new FileRenderer(!serverConfig.isDevelopment()))
        .add(Renderer.typeOf(promiseRenderer.getType()), promiseRenderer)
        .add(Renderer.typeOf(publisherRenderer.getType()), publisherRenderer)
        .add(Renderer.typeOf(Renderable.class), new RenderableRenderer())
        .add(Renderer.typeOf(CharSequence.class), new CharSequenceRenderer())
        .add(Renderer.typeOf(JsonRender.class), new JsonRenderer())
        .add(FormParser.class, new FormParser())
        .add(Clock.class, Clock.systemDefaultZone())
        .add(JsonParser.class, new JsonParser())
        .add(RatpackServer.class, ratpackServer)
        .add(ObjectMapper.class, new ObjectMapper())
        // TODO remove Stopper, and just use RatpackServer instead (will need to update perf and gradle tests)
        .add(Stopper.class, () -> uncheck(() -> {
          ratpackServer.stop();
          return null;
        }))
        .add(HttpClient.class, HttpClient.httpClient(PooledByteBufAllocator.DEFAULT, serverConfig.getMaxContentLength(), execController))
        .add(ServerSentEventStreamClient.class, ServerSentEventStreamClient.sseStreamClient(PooledByteBufAllocator.DEFAULT, execController))
        .add(HealthCheckResultsRenderer.class, new HealthCheckResultsRenderer(PooledByteBufAllocator.DEFAULT))
        .add(RequestId.Generator.class, new UuidBasedRequestIdGenerator());

      addConfigObjects(serverConfig, baseRegistryBuilder);
    } catch (Exception e) {
      // Uncheck because it really shouldn't happen
      throw uncheck(e);
    }

    if (serverConfig.isHasBaseDir()) {
      baseRegistryBuilder.add(FileSystemBinding.class, serverConfig.getBaseDir());
    }

    return baseRegistryBuilder.build();
  }

  private static void addConfigObjects(ServerConfig serverConfig, RegistryBuilder baseRegistryBuilder) {
    for (ConfigObject<?> configObject : serverConfig.getRequiredConfig()) {
      addConfigObject(baseRegistryBuilder, configObject);
    }
  }

  private static <T> void addConfigObject(RegistryBuilder baseRegistryBuilder, ConfigObject<T> configObject) {
    baseRegistryBuilder.add(configObject.getTypeToken(), configObject.getObject());
  }
}
