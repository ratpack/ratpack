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

import com.google.common.base.Throwables;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.error.internal.DefaultProductionErrorHandler;
import ratpack.error.internal.ErrorHandler;
import ratpack.exec.ExecController;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.ActivationBackedMimeTypes;
import ratpack.file.internal.DefaultFileRenderer;
import ratpack.form.internal.FormParser;
import ratpack.func.Function;
import ratpack.handling.Redirector;
import ratpack.handling.internal.DefaultRedirector;
import ratpack.http.client.HttpClient;
import ratpack.launch.LaunchException;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.internal.CharSequenceRenderer;
import ratpack.render.internal.PromiseRenderer;
import ratpack.render.internal.PublisherRenderer;
import ratpack.render.internal.RenderableRenderer;
import ratpack.server.PublicAddress;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.server.Stopper;

import static ratpack.util.ExceptionUtils.uncheck;
import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME;
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME;

public abstract class ServerRegistry {
  public static Registry serverRegistry(RatpackServer ratpackServer, ExecController execController, ServerConfig serverConfig, Function<? super Registry, ? extends Registry> userRegistryFactory) {
    Registry baseRegistry = buildBaseRegistry(ratpackServer, execController, serverConfig);
    Registry userRegistry = buildUserRegistry(userRegistryFactory, baseRegistry);
    return baseRegistry.join(userRegistry);
  }

  private static Registry buildUserRegistry(Function<? super Registry, ? extends Registry> userRegistryFactory, Registry baseRegistry) {
    Registry userRegistry;
    try {
      userRegistry = userRegistryFactory.apply(baseRegistry);
    } catch (Exception e) {
      Throwables.propagateIfPossible(e);
      throw new LaunchException("Failed to build user registry", e);
    }
    return userRegistry;
  }

  private static Registry buildBaseRegistry(RatpackServer ratpackServer, ExecController execController, ServerConfig serverConfig) {
    ErrorHandler errorHandler = serverConfig.isDevelopment() ? new DefaultDevelopmentErrorHandler() : new DefaultProductionErrorHandler();

    RegistryBuilder baseRegistryBuilder;
    try {
      baseRegistryBuilder = Registries.registry()
        .add(ServerConfig.class, serverConfig)
        .add(ByteBufAllocator.class, PooledByteBufAllocator.DEFAULT)
        .add(ExecController.class, execController)
        .add(MimeTypes.class, new ActivationBackedMimeTypes())
        .add(PublicAddress.class, new DefaultPublicAddress(serverConfig.getPublicAddress(), serverConfig.getSSLContext() == null ? HTTP_SCHEME : HTTPS_SCHEME))
        .add(Redirector.class, new DefaultRedirector())
        .add(ClientErrorHandler.class, errorHandler)
        .add(ServerErrorHandler.class, errorHandler)
        .with(new DefaultFileRenderer().register())
        .with(new PromiseRenderer().register())
        .with(new PublisherRenderer().register())
        .with(new RenderableRenderer().register())
        .with(new CharSequenceRenderer().register())
        .add(FormParser.class, FormParser.multiPart())
        .add(FormParser.class, FormParser.urlEncoded())
        .add(RatpackServer.class, ratpackServer)
          // TODO remove Stopper, and just use RatpackServer instead (will need to update perf and gradle tests)
        .add(Stopper.class, () -> uncheck(() -> {
          ratpackServer.stop();
          return null;
        }))
        .add(HttpClient.class, HttpClient.httpClient(execController, PooledByteBufAllocator.DEFAULT, serverConfig.getMaxContentLength()));
    } catch (Exception e) {
      // Uncheck because it really shouldn't happen
      throw uncheck(e);
    }

    if (serverConfig.isHasBaseDir()) {
      baseRegistryBuilder.add(FileSystemBinding.class, serverConfig.getBaseDir());
    }

    return baseRegistryBuilder.build();
  }
}
