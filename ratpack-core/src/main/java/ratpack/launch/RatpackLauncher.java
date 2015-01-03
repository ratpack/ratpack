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

package ratpack.launch;

import static ratpack.util.ExceptionUtils.uncheck;
import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME;
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.error.internal.DefaultDevelopmentErrorHandler;
import ratpack.error.internal.DefaultProductionErrorHandler;
import ratpack.error.internal.ErrorHandler;
import ratpack.exec.ExecController;
import ratpack.exec.internal.DefaultExecController;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.ActivationBackedMimeTypes;
import ratpack.file.internal.DefaultFileRenderer;
import ratpack.form.internal.FormParser;
import ratpack.func.Action;
import ratpack.handling.Redirector;
import ratpack.handling.internal.DefaultRedirector;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClients;
import ratpack.registry.*;
import ratpack.registry.internal.CachingRegistry;
import ratpack.render.internal.CharSequenceRenderer;
import ratpack.render.internal.PromiseRenderer;
import ratpack.render.internal.PublisherRenderer;
import ratpack.render.internal.RenderableRenderer;
import ratpack.server.PublicAddress;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerBuilder;
import ratpack.server.internal.DefaultPublicAddress;

/**
 * Creates and configures a Ratpack application.
 *
 * <pre class="java">{@code
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.server.RatpackServer;
 * import ratpack.launch.RatpackLauncher;
 * import ratpack.launch.ServerConfigBuilder;
 *
 * public class Example {
 *  public static class MyHandler implements Handler {
 *    public void handle(Context context) throws Exception {
 *    }
 *  }
 *
 *  public static void main(String[] args) throws Exception {
 *    RatpackServer server = RatpackLauncher.with(ServerConfigBuilder.noBaseDir().port(5060).build())
 *      .bindings(r -> {
 *        r.add(MyHandler.class, new MyHandler());
 *      }).build(registry -> {
 *        return registry.get(MyHandler.class);
 *      });
 *    server.start();
 *
 *    assert server.isRunning();
 *    assert server.getBindPort() == 5060;
 *
 *    server.stop();
 *  }
 * }
 * }</pre>
 */
public abstract class RatpackLauncher {

  /**
   * Create a RatpackLauncher instance with the supplied configuration.
   *
   * @param config the server configuration for the application
   *
   * @return a launcher instance that can be used to build a {@link ratpack.server.RatpackServer}.
   */
  public static RatpackLauncher with(ServerConfig config) {
      return new DefaultRatpackLauncher(config);
  }

  /**
   * Create a RatpackLauncher instance configured with default options.
   *
   * @return a launcher instance that can be used to build a {@link ratpack.server.RatpackServer}.
   */
  public static RatpackLauncher withDefaults() {
    return with(ServerConfigBuilder.noBaseDir().build());
  }

  /**
   * Create a base registry for an application using the specified server configuration and user define overrides.
   *
   * @param serverConfig the server configuration for this application
   * @param userRegistry the user defined registry contents that will override any default registry entries
   * @return a fully defined registry that can be used to launch an application
   *
   * @throws Exception if there is an error creating the registry.
   */
  public static Registry baseRegistry(ServerConfig serverConfig, Registry userRegistry) throws Exception {
    ErrorHandler errorHandler = serverConfig.isDevelopment() ? new DefaultDevelopmentErrorHandler() : new DefaultProductionErrorHandler();

    Registry baseRegistry = Registries.registry()
      .add(ServerConfig.class, serverConfig)
      .add(ByteBufAllocator.class, PooledByteBufAllocator.DEFAULT)
      .add(ExecController.class, new DefaultExecController(serverConfig.getThreads()))
      .build();

    RegistryBuilder registryBuilder = Registries.registry()
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
      .add(HttpClient.class, HttpClients.httpClient(serverConfig, baseRegistry.join(userRegistry)));

    if (serverConfig.isHasBaseDir()) {
      registryBuilder.add(FileSystemBinding.class, serverConfig.getBaseDir());
    }

    return new CachingRegistry(baseRegistry.join(registryBuilder.build()).join(userRegistry));
  }

  /**
   * Builds a {@link ratpack.server.RatpackServer} with the supplied root handler and backed by the configured root registry.
   *
   * @param factory a function that generates the root handler for the application
   *
   * @return a new, not yet started Ratpack server.
   */
  public abstract RatpackServer build(HandlerFactory factory);

  /**
   * Configure the contents of the base registry for the application. Any values specified here will superseed any defaults.
   *
   * @param action the configuration applied to the registry
   *
   * @return this
   * @throws Exception if the spec errors when being applied.
   */
  public abstract RatpackLauncher bindings(Action<? super RegistrySpec> action) throws Exception;

  private static class DefaultRatpackLauncher extends RatpackLauncher {

    private ServerConfig serverConfig;
    private Registry userRegistry;

    DefaultRatpackLauncher(ServerConfig serverConfig) {
      this.serverConfig = serverConfig;
      this.userRegistry = Registries.empty();
    }

    @Override
    public RatpackServer build(HandlerFactory factory) {
      try {
        Registry baseRegistry = baseRegistry(serverConfig, userRegistry);
        return RatpackServerBuilder.build(baseRegistry, factory);
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    @Override
    public RatpackLauncher bindings(Action<? super RegistrySpec> action) throws Exception {
      userRegistry = Registries.registry(action);
      return this;
    }

  }

}
