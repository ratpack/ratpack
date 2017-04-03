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

package ratpack.groovy.handling.internal;

import com.google.common.reflect.TypeToken;
import groovy.lang.Closure;
import ratpack.api.NonBlocking;
import ratpack.api.Nullable;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.groovy.handling.DefaultGroovyByContentSpec;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.handling.*;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.TypedData;
import ratpack.parse.Parse;
import ratpack.path.PathBinding;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

public class DefaultGroovyContext implements GroovyContext {

  private final Context delegate;

  public DefaultGroovyContext(Context delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyContext getContext() {
    return this;
  }

  @Override
  public void byMethod(Closure<?> closure) throws Exception {
    delegate.byMethod(s -> ClosureUtil.configureDelegateFirst(new DefaultGroovyByMethodSpec(s), closure));
  }

  @Override
  public void byContent(Closure<?> closure) throws Exception {
    delegate.byContent(s -> ClosureUtil.configureDelegateFirst(new DefaultGroovyByContentSpec(s), closure));
  }

  @Override
  public void onClose(Closure<?> callback) {
    onClose(ClosureUtil.delegatingAction(callback));
  }

  @Override
  public Execution getExecution() {
    return delegate.getExecution();
  }

  @Override
  public ServerConfig getServerConfig() {
    return delegate.getServerConfig();
  }

  @Override
  public Request getRequest() {
    return delegate.getRequest();
  }

  @Override
  public Response getResponse() {
    return delegate.getResponse();
  }

  @Override
  @NonBlocking
  public void next() {
    delegate.next();
  }

  @Override
  @NonBlocking
  public void next(Registry registry) {
    delegate.next(registry);
  }

  @Override
  @NonBlocking
  public void insert(Handler... handlers) {
    delegate.insert(handlers);
  }

  @Override
  @NonBlocking
  public void insert(Registry registry, Handler... handlers) {
    delegate.insert(registry, handlers);
  }

  @Override
  public void byMethod(Action<? super ByMethodSpec> action) throws Exception {
    delegate.byMethod(action);
  }

  @Override
  public void byContent(Action<? super ByContentSpec> action) throws Exception {
    delegate.byContent(action);
  }

  @Override
  @NonBlocking
  public void error(Throwable throwable) throws NotInRegistryException {
    delegate.error(throwable);
  }

  @Override
  @NonBlocking
  public void clientError(int statusCode) throws NotInRegistryException {
    delegate.clientError(statusCode);
  }

  @Override
  @NonBlocking
  public void render(Object object) {
    delegate.render(object);
  }

  @Override
  public void redirect(Object to) throws NotInRegistryException {
    delegate.redirect(to);
  }

  @Override
  public void redirect(int code, Object to) throws NotInRegistryException {
    delegate.redirect(code, to);
  }

  @Override
  @NonBlocking
  public void lastModified(Instant lastModified, Runnable runnable) {
    delegate.lastModified(lastModified, runnable);
  }

  @Override
  public <T> Promise<T> parse(Class<T> type) {
    return delegate.parse(type);
  }

  @Override
  public <T> Promise<T> parse(TypeToken<T> type) {
    return delegate.parse(type);
  }

  @Override
  public <T, O> Promise<T> parse(Class<T> type, O options) {
    return delegate.parse(type, options);
  }

  @Override
  public <T, O> Promise<T> parse(TypeToken<T> type, O options) {
    return delegate.parse(type, options);
  }

  @Override
  public <T, O> Promise<T> parse(Parse<T, O> parse) {
    return delegate.parse(parse);
  }

  @Override
  public <T, O> T parse(TypedData body, Parse<T, O> parse) throws Exception {
    return delegate.parse(body, parse);
  }

  @Override
  public DirectChannelAccess getDirectChannelAccess() {
    return delegate.getDirectChannelAccess();
  }

  @Override
  public PathBinding getPathBinding() {
    return delegate.getPathBinding();
  }

  @Override
  public void onClose(Action<? super RequestOutcome> callback) {
    delegate.onClose(callback);
  }

  @Override
  public Path file(String path) throws NotInRegistryException {
    return delegate.file(path);
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    return delegate.get(type);
  }

  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    return delegate.get(type);
  }

  @Override
  public <O> Optional<O> maybeGet(Class<O> type) {
    return delegate.maybeGet(type);
  }

  @Override
  @Nullable
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return delegate.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(Class<O> type) {
    return delegate.getAll(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return delegate.getAll(type);
  }

  @Override
  public <T, O> Optional<O> first(TypeToken<T> type, Function<? super T, ? extends O> function) throws Exception {
    return delegate.first(type, function);
  }

}
