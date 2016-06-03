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
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.inject.*;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import io.netty.buffer.ByteBufAllocator;
import org.aopalliance.intercept.MethodInterceptor;
import org.reactivestreams.Publisher;
import ratpack.api.Blocks;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.exec.ExecController;
import ratpack.exec.ExecInitializer;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.form.internal.FormParser;
import ratpack.guice.ExecutionScoped;
import ratpack.guice.RequestScoped;
import ratpack.handling.Redirector;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.client.HttpClient;
import ratpack.registry.Registry;
import ratpack.render.Renderable;
import ratpack.render.Renderer;
import ratpack.server.PublicAddress;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.sse.ServerSentEventStreamClient;

import java.lang.reflect.Method;
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
    ExecutionScope executionScope = new ExecutionScope();
    bindScope(ExecutionScoped.class, executionScope);
    RequestScope requestScope = new RequestScope();
    bindScope(RequestScoped.class, requestScope);
    bind(ExecutionScope.class).toInstance(executionScope);
    bind(RequestScope.class).toInstance(requestScope);

    bind(ExecutionPrimingInitializer.class);

    List<Class<?>> simpleTypes = ImmutableList.of(
      ServerConfig.class, ByteBufAllocator.class, ExecController.class, MimeTypes.class, PublicAddress.class,
      Redirector.class, ClientErrorHandler.class, ServerErrorHandler.class, RatpackServer.class
    );
    List<TypeToken<?>> genericTypes = ImmutableList.of(
      new TypeToken<Renderer<Path>>() {}, new TypeToken<Renderer<Promise>>() {}, new TypeToken<Renderer<Publisher>>() {},
      new TypeToken<Renderer<Renderable>>() {}, new TypeToken<Renderer<CharSequence>>() {}
    );
    List<Class<?>> setTypes = ImmutableList.of(FormParser.class);
    List<Class<?>> optionalTypes = ImmutableList.of(FileSystemBinding.class);

    simpleTypes.stream().forEach(t -> simpleBind(t));
    genericTypes.stream().forEach(t -> genericBind(t));
    setTypes.stream().forEach(t -> setBind(t));
    optionalTypes.stream().forEach(t -> optionalBind(t));

    MethodInterceptor interceptor = new BlockingInterceptor();
    bindInterceptor(Matchers.annotatedWith(Blocks.class), new NotGroovyMethodMatcher(), interceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Blocks.class), interceptor);
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
  HttpClient httpClient(ByteBufAllocator byteBufAllocator, ServerConfig serverConfig, ExecController execController) {
    return HttpClient.httpClient(byteBufAllocator, serverConfig.getMaxContentLength(), execController);
  }

  @Provides
  ServerSentEventStreamClient sseClient(ByteBufAllocator byteBufAllocator, ExecController execController) {
    return ServerSentEventStreamClient.sseStreamClient(byteBufAllocator, execController);
  }

  @Provides
  @ExecutionScoped
  Execution execution() {
    return Execution.current();
  }

  @Provides
  @RequestScoped
  Request request(Execution execution) throws Throwable {
    return execution.maybeGet(Request.class).orElseThrow(() -> {
      throw new RuntimeException("Cannot inject Request in execution scope as execution has no request object - this execution is not processing a request");
    });
  }

  @Provides
  @RequestScoped
  Response response(Execution execution) throws Throwable {
    return execution.maybeGet(Response.class).orElseThrow(() -> {
      throw new RuntimeException("Cannot inject Response in execution scope as execution has no response object - this execution is not processing a request");
    });
  }

  @Singleton
  static class ExecutionPrimingInitializer implements ExecInitializer {

    private final List<Key<?>> executionScope;
    private final List<Key<?>> requestScope;
    private final Injector injector;

    @Inject
    public ExecutionPrimingInitializer(ExecutionScope executionScope, RequestScope requestScope, Injector injector) {
      this.executionScope = ImmutableList.copyOf(Iterables.filter(executionScope.getKeys(), input -> !input.getTypeLiteral().getRawType().equals(Execution.class)));
      this.requestScope = ImmutableList.copyOf(Iterables.filter(requestScope.getKeys(), input -> !input.getTypeLiteral().getRawType().equals(Request.class) && !input.getTypeLiteral().getRawType().equals(Response.class)));
      this.injector = injector;
    }

    @Override
    public void init(Execution execution) {
      for (Key<?> key : executionScope) {
        doAdd(execution, key);
      }
      for (Key<?> key : requestScope) {
        doAdd(execution, key);
      }
    }

    public <T> void doAdd(Execution execution, Key<T> key) {
      execution.addLazy(GuiceUtil.toTypeToken(key.getTypeLiteral()), injector.getProvider(key)::get);
    }
  }

  private static class NotGroovyMethodMatcher extends AbstractMatcher<Method> {
    @Override
    public boolean matches(Method method) {
      return !method.isSynthetic();
    }
  }
}
