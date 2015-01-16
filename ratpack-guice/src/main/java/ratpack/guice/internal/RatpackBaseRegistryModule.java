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

package ratpack.guice.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.exec.*;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.form.internal.FormParser;
import ratpack.handling.Redirector;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClients;
import ratpack.registry.Registry;
import ratpack.render.Renderable;
import ratpack.render.Renderer;
import ratpack.server.PublicAddress;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Expose bindings for objects in the base registry.
 * This should be kept in sync with the base registry assembled in {@link ratpack.server.internal.ServerRegistry}.
 */
public class RatpackBaseRegistryModule extends AbstractModule {

  private final Registry baseRegistry;

  public RatpackBaseRegistryModule(Registry baseRegistry) {
    this.baseRegistry = baseRegistry;
  }

  @SuppressWarnings({"Convert2MethodRef", "rawtypes"})
  @Override
  protected void configure() {
    List<Class<?>> simpleTypes = ImmutableList.of(
      ServerConfig.class, ByteBufAllocator.class, ExecController.class, MimeTypes.class, PublicAddress.class, Redirector.class,
      ClientErrorHandler.class, ServerErrorHandler.class, RatpackServer.class
    );
    List<TypeToken<?>> genericTypes = ImmutableList.of(
      new TypeToken<Renderer<Path>>() {}, new TypeToken<Renderer<SuccessPromise>>() {}, new TypeToken<Renderer<Publisher>>() {},
      new TypeToken<Renderer<Renderable>>() {}, new TypeToken<Renderer<CharSequence>>() {}
    );
    List<Class<?>> setTypes = ImmutableList.of(FormParser.class);
    List<Class<?>> optionalTypes = ImmutableList.of(FileSystemBinding.class);

    simpleTypes.stream().forEach(t -> simpleBind(t));
    genericTypes.stream().forEach(t -> genericBind(t));
    setTypes.stream().forEach(t -> setBind(t));
    optionalTypes.stream().forEach(t -> optionalBind(t));
  }

  private <T> void simpleBind(Class<T> type) {
    bind(type).toProvider(() -> baseRegistry.get(type));
  }

  @SuppressWarnings("unchecked")
  private <T> void genericBind(TypeToken<T> typeToken) {
    TypeLiteral<T> typeLiteral = (TypeLiteral<T>) TypeLiteral.get(typeToken.getType());
    bind(typeLiteral).toProvider(() -> baseRegistry.get(typeToken));
  }

  private <T> void setBind(Class<T> type) {
    Multibinder<T> setBinder = Multibinder.newSetBinder(binder(), type);
    baseRegistry.getAll(type).forEach(instance -> setBinder.addBinding().toInstance(instance));
  }

  private <T> void optionalBind(Class<T> type) {
    Optional<T> optional = baseRegistry.maybeGet(type);
    if (optional.isPresent()) {
      bind(type).toProvider(() -> baseRegistry.get(type));
    }
  }

  @Provides
  ExecControl execControl(ExecController execController) {
    return execController.getControl();
  }

  @Provides
  Execution execution(ExecControl execControl) {
    try {
      return execControl.getExecution();
    } catch (ExecutionException e) {
      throw new OutOfScopeException("Cannot provide an instance of " + Execution.class.getName() + " as none is bound to the current thread (are you outside of a managed thread?)");
    }
  }

  @Provides
  HttpClient httpClient(ExecController execController, ByteBufAllocator byteBufAllocator, ServerConfig serverConfig) {
    return HttpClients.httpClient(execController, byteBufAllocator, serverConfig.getMaxContentLength());
  }

}
