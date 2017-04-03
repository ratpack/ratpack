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

package ratpack.groovy.handling;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import ratpack.file.FileHandlerSpec;
import ratpack.func.Action;
import ratpack.func.Predicate;
import ratpack.groovy.handling.internal.DefaultGroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

import static ratpack.groovy.Groovy.chainAction;
import static ratpack.groovy.Groovy.groovyHandler;
import static ratpack.groovy.internal.ClosureUtil.delegatingAction;

/**
 * A Groovy oriented handler chain builder DSL.
 * <p>
 * The methods specific to this subclass create {@link ratpack.handling.Handler} instances from closures and
 * add them to the underlying chain.
 * <p>
 * These methods are generally shortcuts for {@link #all(ratpack.handling.Handler)} on this underlying chain.
 */
public interface GroovyChain extends Chain {

  /**
   * Creates a Groovy chain wrapper over a chain instance.
   *
   * @param chain a chain instance
   * @return a Groovy wrapper
   */
  static GroovyChain from(Chain chain) {
    if (chain instanceof GroovyChain) {
      return (GroovyChain) chain;
    } else {
      return new DefaultGroovyChain(chain);
    }
  }

  /**
   * Adds the given {@code Closure} as a {@code Handler} to this {@code GroovyChain}.
   *
   * @param handler the {@code Closure} to add
   * @return this {@code GroovyChain}
   */
  default GroovyChain all(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return all(groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyChain all(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain all(Class<? extends Handler> handler) {
    return all(getRegistry().get(handler));
  }

  /**
   * Creates a handler from the given closure.
   *
   * @param closure a chain definition
   * @return a new handler
   * @throws Exception any thrown by {@code closure}
   */
  default Handler chain(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return chain(c -> ClosureUtil.configureDelegateFirst(GroovyChain.from(c), closure));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code DELETE}.
   * <p>
   * See {@link GroovyChain#delete(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain delete(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return delete(path, groovyHandler(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code DELETE} and the {@code path} is at the current root.
   * <p>
   * See {@link GroovyChain#delete(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain delete(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return delete(groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain delete(String path, Handler handler) {
    return from(Chain.super.delete(path, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain delete(String path, Class<? extends Handler> handler) {
    return delete(path, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain delete(Handler handler) {
    return from(Chain.super.delete(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain delete(Class<? extends Handler> handler) {
    return delete(getRegistry().get(handler));
  }

  /**
   * Creates a {@code List} of {@code Handler} from the given {@code Closure} and adds a {@code Handler} to this {@code GroovyChain} that
   * changes the {@link ratpack.file.FileSystemBinding} for the {@code Handler} list.
   * <p>
   * See {@link GroovyChain#fileSystem(String, ratpack.func.Action)} for more details.
   *
   * @param path the relative {@code path} to the new file system binding point
   * @param handlers the definition of the handler chain
   * @return this {@code GroovyChain}
   * @throws Exception any thrown by {@code closure}
   */
  default GroovyChain fileSystem(String path, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) throws Exception {
    return fileSystem(path, chainAction(handlers));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain fileSystem(String path, Action<? super Chain> action) throws Exception {
    return from(Chain.super.fileSystem(path, action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain fileSystem(String path, Class<? extends Action<? super Chain>> action) throws Exception {
    return fileSystem(path, getRegistry().get(action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain files(Action<? super FileHandlerSpec> config) throws Exception {
    return from(Chain.super.files(config));
  }

  @Override
  default GroovyChain files() {
    return from(Chain.super.files());
  }


  default GroovyChain files(@DelegatesTo(value = FileHandlerSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return files(delegatingAction(FileHandlerSpec.class, closure));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain get(String path, Handler handler) {
    return from(Chain.super.get(path, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain get(String path, Class<? extends Handler> handler) {
    return get(path, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain get(Handler handler) {
    return from(Chain.super.get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain get(Class<? extends Handler> handler) {
    return get(getRegistry().get(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code GET}.
   * <p>
   * See {@link GroovyChain#get(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain get(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return get(path, groovyHandler(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code GET} and the {@code path} is at the current root.
   * <p>
   * See {@link GroovyChain#get(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain get(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return get(groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  default GroovyChain host(String hostName, Action<? super Chain> action) throws Exception {
    return from(Chain.super.host(hostName, action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain host(String hostName, Class<? extends Action<? super Chain>> action) throws Exception {
    return host(hostName, getRegistry().get(action));
  }

  /**
   * If the request has a {@code Host} header that matches the given host name exactly, handling will be delegated to the chain defined by the given closure.
   *
   * @param hostName the name of the HTTP Header to match on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   * @throws Exception any thrown by {@code closure}
   * @see #host(String, ratpack.func.Action)
   */
  default GroovyChain host(String hostName, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) throws Exception {
    return host(hostName, chainAction(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain insert(Action<? super Chain> action) throws Exception {
    return from(Chain.super.insert(action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain insert(Class<? extends Action<? super Chain>> action) throws Exception {
    return insert(getRegistry().get(action));
  }

  /**
   * Inserts the given nested handler chain.
   * <p>
   * Shorter form of {@link #all(Handler)} handler}({@link #chain(groovy.lang.Closure) chain}({@code closure}).
   *
   * @param closure the handler chain to insert
   * @return this
   * @throws Exception any thrown by {@code closure}
   */
  default GroovyChain insert(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return insert(chainAction(closure));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain patch(String path, Handler handler) {
    return from(Chain.super.patch(path, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain patch(String path, Class<? extends Handler> handler) {
    return patch(path, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain patch(Handler handler) {
    return from(Chain.super.patch(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain patch(Class<? extends Handler> handler) {
    return patch(getRegistry().get(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code PATCH}.
   * <p>
   * See {@link GroovyChain#put(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain patch(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return patch(path, groovyHandler(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code PATCH} and the {@code path} is at the current root.
   * <p>
   * See {@link GroovyChain#put(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain patch(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return path(groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain options(String path, Handler handler) {
    return from(Chain.super.options(path, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain options(String path, Class<? extends Handler> handler) {
    return options(path, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain options(Handler handler) {
    return from(Chain.super.options(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain options(Class<? extends Handler> handler) {
    return options(getRegistry().get(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code OPTIONS}.
   * <p>
   * See {@link GroovyChain#put(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   * @since 1.1
   */
  default GroovyChain options(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return options(path, groovyHandler(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code OPTIONS} and the {@code path} is at the current root.
   * <p>
   * See {@link GroovyChain#put(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   * @since 1.1
   */
  default GroovyChain options(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return path(groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain path(String path, Handler handler) {
    return from(Chain.super.path(path, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain path(Handler handler) {
    return path("", handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain path(String path, Class<? extends Handler> handler) {
    return path(path, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain path(Class<? extends Handler> handler) {
    return path("", handler);
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} exactly.
   * <p>
   * See {@link GroovyChain#path(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match exactly on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain path(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return path(path, groovyHandler(handler));
  }

  default GroovyChain path(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return path("", handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain post(String path, Handler handler) {
    return from(Chain.super.post(path, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain post(String path, Class<? extends Handler> handler) {
    return post(path, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain post(Handler handler) {
    return from(Chain.super.post(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain post(Class<? extends Handler> handler) {
    return post(getRegistry().get(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code POST}.
   * <p>
   * See {@link GroovyChain#post(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain post(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return post(path, groovyHandler(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code POST} and the {@code path} is at the current root.
   * <p>
   * See {@link GroovyChain#post(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain post(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return post(groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain prefix(String prefix, Action<? super Chain> action) throws Exception {
    return from(Chain.super.prefix(prefix, action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain prefix(String prefix, Class<? extends Action<? super Chain>> action) throws Exception {
    return prefix(prefix, getRegistry().get(action));
  }

  /**
   * Creates a {@code List} of {@code Handler} from the given {@code Closure} and adds a {@code Handler} to
   * this {@code GroovyChain} that delegates to the {@code Handler} list if the relative path starts with the given
   * {@code prefix}.
   * <p>
   * See {@link Chain#prefix(String, ratpack.func.Action)} for more details.
   *
   * @param prefix the relative path to match on
   * @param chain the definition of the chain to delegate to
   * @return this {@code GroovyChain}
   * @throws Exception any exception thrown by the given closure
   */
  default GroovyChain prefix(String prefix, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> chain) throws Exception {
    return prefix(prefix, chainAction(chain));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain put(String path, Handler handler) {
    return from(Chain.super.put(path, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain put(String path, Class<? extends Handler> handler) {
    return put(path, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain put(Handler handler) {
    return from(Chain.super.put(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain put(Class<? extends Handler> handler) {
    return put(getRegistry().get(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code PUT}.
   * <p>
   * See {@link GroovyChain#put(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain put(String path, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return put(path, groovyHandler(handler));
  }

  /**
   * Adds a {@code Handler} to this {@code GroovyChain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code PUT} and the {@code path} is at the current root.
   * <p>
   * See {@link GroovyChain#put(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code GroovyChain}
   */
  default GroovyChain put(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return put(groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  default GroovyChain redirect(int code, String location) {
    return from(Chain.super.redirect(code, location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain register(Registry registry) {
    return from(Chain.super.register(registry));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain register(Action<? super RegistrySpec> action) throws Exception {
    return from(Chain.super.register(action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain register(Registry registry, Action<? super Chain> action) throws Exception {
    return from(Chain.super.register(registry, action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain register(Registry registry, Class<? extends Action<? super Chain>> action) throws Exception {
    return register(registry, getRegistry().get(action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> chainAction) throws Exception {
    return from(Chain.super.register(registryAction, chainAction));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain register(Action<? super RegistrySpec> registryAction, Class<? extends Action<? super Chain>> action) throws Exception {
    return register(registryAction, getRegistry().get(action));
  }

  default GroovyChain register(Action<? super RegistrySpec> registryAction, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) throws Exception {
    return register(registryAction, chainAction(handler));
  }

  default GroovyChain register(Registry registry, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) throws Exception {
    return register(registry, chainAction(handlers));
  }

  default GroovyChain register(@DelegatesTo(value = RegistrySpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return register(delegatingAction(closure));
  }

  @Override
  default GroovyChain when(Predicate<? super Context> test, Action<? super Chain> action) throws Exception {
    return from(Chain.super.when(test, action));
  }

  @Override
  default GroovyChain when(Predicate<? super Context> test, Class<? extends Action<? super Chain>> action) throws Exception {
    return from(Chain.super.when(test, action));
  }

  default GroovyChain when(
    Predicate<? super Context> test,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers
  ) throws Exception {
    return when(test, chainAction(handlers));
  }

  default GroovyChain when(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers
  ) throws Exception {
    return when(test, chainAction(handlers));
  }

  default GroovyChain when(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    Action<? super Chain> chain
  ) throws Exception {
    return when(
      ctx -> {
        final GroovyContext groovyContext = GroovyContext.from(ctx);
        return DefaultGroovyMethods.asBoolean(
          ClosureUtil.cloneAndSetDelegate(groovyContext, test, Closure.DELEGATE_FIRST).isCase(groovyContext)
        );
      },
      chain
    );
  }

  default GroovyChain when(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    Class<? extends Action<? super Chain>> action
  ) throws Exception {
    return when(test, getRegistry().get(action));
  }

  /**
   * Inlines the given handlers if {@code test} is {@code true}.
   * <p>
   * This is literally just sugar for wrapping the given action in an {@code if} statement.
   * It can be useful when conditionally adding handlers based on state available when building the chain.
   *
   * @param test whether to include the given chain action
   * @param handlers the handlers to maybe include
   * @return this
   * @throws Exception any thrown by {@code action}
   * @since 1.4
   * @see Chain#when(boolean, Action)
   */
  default GroovyChain when(boolean test, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) throws Exception {
    return when(test, chainAction(handlers));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain when(boolean test, Action<? super Chain> action) throws Exception {
    return from(Chain.super.when(test, action));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain when(boolean test, Class<? extends Action<? super Chain>> action) throws Exception {
    return from(Chain.super.when(test, action));
  }

  @Override
  default GroovyChain when(Predicate<? super Context> test, Action<? super Chain> onTrue, Action<? super Chain> onFalse) throws Exception {
    return from(Chain.super.when(test, onTrue, onFalse));
  }

  @Override
  default GroovyChain when(Predicate<? super Context> test, Class<? extends Action<? super Chain>> onTrue, Class<? extends Action<? super Chain>> onFalse) throws Exception {
    return from(Chain.super.when(test, onTrue, onFalse));
  }

  default GroovyChain when(
    Predicate<? super Context> test,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> ifHandlers,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> elseHandlers
  ) throws Exception {
    return when(test, chainAction(ifHandlers), chainAction(elseHandlers));
  }

  default GroovyChain when(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> ifHandlers,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> elseHandlers
  ) throws Exception {
    return when(test, chainAction(ifHandlers), chainAction(elseHandlers));
  }

  default GroovyChain when(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    Action<? super Chain> ifChain,
    Action<? super Chain> elseChain
  ) throws Exception {
    return when(
      ctx -> {
        final GroovyContext groovyContext = GroovyContext.from(ctx);
        return DefaultGroovyMethods.asBoolean(
          ClosureUtil.cloneAndSetDelegate(groovyContext, test, Closure.DELEGATE_FIRST).isCase(groovyContext)
        );
      },
      ifChain,
      elseChain
    );
  }

  default GroovyChain when(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    Class<? extends Action<? super Chain>> ifAction,
    Class<? extends Action<? super Chain>> elseAction
  ) throws Exception {
    return when(test, getRegistry().get(ifAction), getRegistry().get(elseAction));
  }

  /**
   * Inlines the appropriate handlers based on the given {@code test}.
   * <p>
   * A value of {@code true} will result in the given {@code ifHandlers} being used.
   * A value of {@code false} will result in the given {@code elseHandlers} being used.
   * <p>
   * This is literally just sugar for wrapping the given action in an if/else statement.
   * It can be useful when conditionally adding handlers based on state available when building the chain.
   *
   * @param test predicate to decide which handlers to include
   * @param ifHandlers the handlers to include when the test is true
   * @param elseHandlers the handlers to include when the test is false
   * @return this
   * @throws Exception any thrown by {@code action}
   * @since 1.5
   * @see Chain#when(boolean, Action, Action)
   */
  default GroovyChain when(
    boolean test,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> ifHandlers,
    @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> elseHandlers
  ) throws Exception {
    return when(test, chainAction(ifHandlers), chainAction(elseHandlers));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain when(boolean test, Action<? super Chain> onTrue, Action<? super Chain> onFalse) throws Exception {
    return from(Chain.super.when(test, onTrue, onFalse));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain when(boolean test, Class<? extends Action<? super Chain>> onTrue, Class<? extends Action<? super Chain>> onFalse) throws Exception {
    return from(Chain.super.when(test, onTrue, onFalse));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain onlyIf(Predicate<? super Context> test, Handler handler) {
    return from(Chain.super.onlyIf(test, handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain onlyIf(Predicate<? super Context> test, Class<? extends Handler> handler) {
    return from(Chain.super.onlyIf(test, handler));
  }

  default GroovyChain onlyIf(
    Predicate<? super Context> test,
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler
  ) {
    return onlyIf(test, groovyHandler(handler));
  }

  default GroovyChain onlyIf(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler
  ) {
    return onlyIf(test, groovyHandler(handler));
  }

  default GroovyChain onlyIf(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    Handler handler
  ) {
    return onlyIf(
      ctx -> {
        final GroovyContext groovyContext = GroovyContext.from(ctx);
        return DefaultGroovyMethods.asBoolean(
          ClosureUtil.cloneAndSetDelegate(groovyContext, test, Closure.DELEGATE_FIRST).isCase(groovyContext)
        );
      },
      handler
    );
  }

  default GroovyChain onlyIf(
    @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> test,
    Class<? extends Handler> handler
  ) {
    return onlyIf(test, getRegistry().get(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default GroovyChain notFound() {
    return from(Chain.super.notFound());
  }
}
