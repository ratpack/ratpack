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

package ratpack.session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.AsciiString;
import ratpack.exec.ExecControl;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.ExecutionScoped;
import ratpack.handling.HandlerDecorator;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.server.ServerConfig;
import ratpack.session.internal.*;
import ratpack.session.store.internal.LocalMemorySessionStore;
import ratpack.util.Types;

import javax.inject.Named;
import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Provides support for HTTP sessions.
 * <p>
 * This module provides the general session API (see {@link Session}), and a default {@link SessionStore} implementation that stores session data in local memory.
 *
 * <h3>The session store</h3>
 * <p>
 * It is expected that most applications will provide alternative bindings for the {@link SessionStore} type, overriding the default.
 * This allows arbitrary stores to be used to persist session data.
 * <p>
 * The default, in memory, implementation stores the data in a {@link Cache}{@code <}{@link AsciiString}, {@link ByteBuf}{@code >}.
 * This cache instance is provided by this module and is a simple cache with no maximum size.
 * As {@link Cache} is a general type, this binding is {@link Named} with {@link #LOCAL_MEMORY_SESSION_CACHE_BINDING_NAME}.
 * Override this binding
 *
 * <h3>Serialization</h3>
 * <p>
 * Objects must be serialized to be stored in the session.
 * The get/set methods {@link SessionData} allow supplying a {@link SessionSerializer} to be used for the specific value.
 * For variants of the get/set methods where a serializer is not provided, the implementation of {@link SessionSerializer} bound with Guice will be used.
 * The default implementation provided by this module uses Java's in built serialization mechanism.
 * Users of this module may choose to override this binding with an alternative serialization strategy.
 * <p>
 * However, other Ratpack extensions may require session storage any rely on Java serialization.
 * For this reason, there is also always a {@link JavaSessionSerializer} implementation available that is guaranteed to be able to serialize any {@link Serializable}
 * object (that conforms to the {@link Serializable} contract.
 * Users of this module may also choose to override this binding with another implementation (e.g. one based on <a href="https://github.com/EsotericSoftware/kryo">Kryo</a>),
 * but this implementation must be able to serialize any object implementing {@link Serializable}.
 *
 * It is also likely that alternative bindings will be provided for {@link SessionSerializer} and {@link JavaSessionSerializer} to provide different serialization strategies.
 * The default binding for both types is an implementation that uses out-of-the-box Java serialization (which is neither fast nor efficient).
 *
 * <h3>Example usage</h3>
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.path.PathTokens;
 * import ratpack.session.Session;
 * import ratpack.session.SessionModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static junit.framework.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(a -> a
 *         .registry(Guice.registry(b -> b
 *             .module(SessionModule.class)
 *         ))
 *         .handlers(c -> c
 *             .get("set/:name/:value", ctx ->
 *                 ctx.get(Session.class).getData().then(sessionData -> {
 *                   PathTokens pathTokens = ctx.getPathTokens();
 *                   sessionData.set(pathTokens.get("name"), pathTokens.get("value"));
 *                   ctx.render("ok");
 *                 })
 *             )
 *             .get("get/:name", ctx -> {
 *               ctx.get(Session.class).getData()
 *                 .map(d -> d.require(ctx.getPathTokens().get("name")))
 *                 .then(ctx::render);
 *             })
 *         )
 *     ).test(httpClient -> {
 *       assertEquals("ok", httpClient.getText("set/foo/bar"));
 *       assertEquals("bar", httpClient.getText("get/foo"));
 *
 *       assertEquals("ok", httpClient.getText("set/foo/baz"));
 *       assertEquals("baz", httpClient.getText("get/foo"));
 *     });
 *   }
 * }
 * }</pre>
 */
public class SessionModule extends ConfigurableModule<SessionIdCookieConfig> {

  public static final String LOCAL_MEMORY_SESSION_CACHE_BINDING_NAME = "localMemorySessionCache";

  public static final Key<Cache<AsciiString, ByteBuf>> LOCAL_MEMORY_SESSION_CACHE_BINDING_KEY = Key.get(
    new TypeLiteral<Cache<AsciiString, ByteBuf>>() {},
    Names.named(LOCAL_MEMORY_SESSION_CACHE_BINDING_NAME)
  );

  public static void memoryStore(Binder binder, Consumer<? super CacheBuilder<AsciiString, ByteBuf>> config) {
    binder.bind(LOCAL_MEMORY_SESSION_CACHE_BINDING_KEY).toProvider(() -> {
      CacheBuilder<AsciiString, ByteBuf> cacheBuilder = Types.cast(CacheBuilder.newBuilder());
      config.accept(cacheBuilder);
      return cacheBuilder.build();
    }).in(Scopes.SINGLETON);
  }

  @Override
  protected SessionIdCookieConfig createConfig(ServerConfig serverConfig) {
    return new DefaultSessionIdCookieConfig();
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().to(StoreSessionIfDirtyHandlerDecorator.class);
    bind(SessionStatus.class).in(ExecutionScoped.class);
    memoryStore(binder(), s -> {
    });
  }

  @Provides
  @Singleton
  SessionStore sessionStoreAdapter(@Named(LOCAL_MEMORY_SESSION_CACHE_BINDING_NAME) Cache<AsciiString, ByteBuf> cache, ExecControl execControl) {
    return new LocalMemorySessionStore(cache, execControl);
  }

  @Provides
  SessionIdGenerator sessionIdGenerator() {
    return new DefaultSessionIdGenerator();
  }

  @Provides
  @ExecutionScoped
  SessionId sessionId(Request request, Response response, SessionIdGenerator idGenerator, SessionIdCookieConfig cookieConfig) {
    return new CookieBasedSessionId(request, response, idGenerator, cookieConfig);
  }

  @Provides
  SessionSerializer sessionValueSerializer(JavaSessionSerializer sessionSerializer) {
    return sessionSerializer;
  }

  @Provides
  JavaSessionSerializer javaSessionSerializer() {
    return new JavaBuiltinSessionSerializer();
  }

  @Provides
  @ExecutionScoped
  Session sessionAdapter(SessionId sessionId, SessionStore store, SessionStatus sessionStatus, ByteBufAllocator bufferAllocator, SessionSerializer defaultSerializer, JavaSessionSerializer javaSerializer) {
    return new DefaultSession(sessionId, bufferAllocator, store, sessionStatus, defaultSerializer, javaSerializer);
  }

}
