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
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.ExecControl;
import ratpack.exec.Promise;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.ExecutionScoped;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.server.ServerConfig;
import ratpack.session.internal.*;
import ratpack.session.store.SessionStoreAdapter;
import ratpack.session.store.internal.LocalMemorySessionStoreAdapter;

import javax.inject.Named;

public class NewSessionModule extends ConfigurableModule<SessionIdCookieConfig> {

  public static final String LOCAL_MEMORY_SESSION_CACHE = "localMemorySessionCache";

  @Override
  protected SessionIdCookieConfig createConfig(ServerConfig serverConfig) {
    return new DefaultSessionIdCookieConfig();
  }

  @Override
  protected void configure() {
    bind(StoreSessionIfDirtyHandlerDecorator.class);
    bind(SessionStatus.class).in(ExecutionScoped.class);
  }

  @Provides
  @Named(LOCAL_MEMORY_SESSION_CACHE)
  Cache<String, ByteBuf> localMemorySessionCache() {
    return CacheBuilder.newBuilder().build();
  }

  @Provides
  @Singleton
  SessionStoreAdapter sessionStoreAdapter(@Named(LOCAL_MEMORY_SESSION_CACHE) Cache<String, ByteBuf> cache, ExecControl execControl) {
    return new LocalMemorySessionStoreAdapter(cache, execControl);
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
  SessionValueSerializer sessionValueSerializer() {
    return new JavaSerializationSessionValueSerializer();
  }

  @Provides
  @ExecutionScoped
  Promise<SessionAdapter> sessionAdapter(SessionId sessionId, SessionStoreAdapter sessionStoreAdapter, SessionStatus sessionStatus, SessionValueSerializer sessionValueSerializer, ByteBufAllocator bufferAllocator) {
    return sessionStoreAdapter.load(sessionId, bufferAllocator)
      .<SessionAdapter>map(data -> new DefaultSessionAdapter(sessionId, bufferAllocator, sessionStoreAdapter, sessionStatus, sessionValueSerializer, data))
      .cache();
  }

}
