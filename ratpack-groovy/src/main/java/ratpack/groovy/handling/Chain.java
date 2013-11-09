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
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.util.Action;

import java.util.List;

/**
 * A Groovy oriented handler chain builder DSL.
 * <p>
 * The methods specific to this subclass create {@link ratpack.handling.Handler} instances from closures and
 * add them to the underlying chain.
 * <p>
 * These methods are generally shortcuts for {@link #handler(ratpack.handling.Handler)} on this underlying chain.
 */
public interface Chain extends ratpack.handling.Chain {

  /**
   * Adds the given {@code Closure} as a {@code Handler} to this {@code Chain}.
   *
   * @param handler the {@code Closure} to add
   * @return this {@code Chain}
   */
  Chain handler(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} exactly.
   * <p>
   * See {@link Chain#handler(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match exactly on
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain handler(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain handler(Handler handler);

  /**
   * Creates a {@code List} of {@code Handler} from the given {@code Closure} and adds a {@code Handler} to
   * this {@code Chain} that delegates to the {@code Handler} list if the relative path starts with the given
   * {@code prefix}.
   * <p>
   * See {@link Chain#prefix(String, ratpack.handling.Handler...)} for more details.
   *
   * @param prefix the relative path to match on
   * @param chain the definition of the chain to delegate to
   * @return this {@code Chain}
   */
  Chain prefix(String prefix, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> chain);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain prefix(String prefix, Handler... handlers);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain prefix(String prefix, List<Handler> handlers);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain prefix(String prefix, Action<? super ratpack.handling.Chain> chainAction);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain handler(String path, Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain get(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain get(String path, Handler handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code GET}.
   * <p>
   * See {@link Chain#get(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain get(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code GET} and the {@code path} is at the current root.
   * <p>
   * See {@link Chain#get(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain get(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain post(String path, Handler handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code POST}.
   * <p>
   * See {@link Chain#post(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain post(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain post(Handler handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code POST} and the {@code path} is at the current root.
   * <p>
   * See {@link Chain#post(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain post(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain put(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain put(String path, Handler handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code PUT}.
   * <p>
   * See {@link Chain#put(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain put(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code PUT} and the {@code path} is at the current root.
   * <p>
   * See {@link Chain#put(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain put(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain delete(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain delete(String path, Handler handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler} if the
   * relative {@code path} matches the given {@code path} and the {@code request} {@code HTTPMethod} is {@code DELETE}.
   * <p>
   * See {@link Chain#delete(String, ratpack.handling.Handler)} for more details.
   *
   * @param path the relative path to match on
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain delete(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler}
   * if the {@code request} {@code HTTPMethod} is {@code DELETE} and the {@code path} is at the current root.
   * <p>
   * See {@link Chain#delete(ratpack.handling.Handler)} for more details.
   *
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain delete(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * Creates a {@code List} of {@code Handler} from the given {@code Closure} and adds a {@code Handler} to this {@code Chain}
   * that inserts the {@code Handler} list with the given {@code service} addition.
   * <p>
   * See {@link Chain#register(Object, java.util.List)} for more details.
   *
   * @param service the object to add to the service for the handlers
   * @param handlers the handlers to register the service with
   * @return this {@code Chain}
   */
  Chain register(Object service, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain register(Object object, List<Handler> handlers);

  /**
   * Creates a {@code List} of {@code Handler} from the given {@code Closure} and adds a {@code Handler} to this {@code Chain} that
   * inserts the the {@code Handler} list with the given {@code service} addition.
   * <p>
   * See {@link Chain#register(Class, Object, java.util.List)} for more details.
   *
   * @param type the {@code Type} by which to make the service object available
   * @param service the object to add to the service for the handlers
   * @param handlers the handlers to register the service with
   * @param <T> the concrete type of the service addition
   * @return this {@code Chain}
   */
  <T> Chain register(Class<? super T> type, T service, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> Chain register(Class<? super T> type, T object, List<Handler> handlers);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain assets(String path, String... indexFiles);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain fileSystem(String path, List<Handler> handlers);

  /**
   * Creates a {@code List} of {@code Handler} from the given {@code Closure} and adds a {@code Handler} to this {@code Chain} that
   * changes the {@link ratpack.file.FileSystemBinding} for the {@code Handler} list.
   * <p>
   * See {@link Chain#fileSystem(String, java.util.List)} for more details.
   *
   * @param path the relative {@code path} to the new file system binding point
   * @param handlers the definition of the handler chain
   * @return this {@code Chain}
   */
  Chain fileSystem(String path, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

  /**
   * {@inheritDoc}
   */
  @Override
  Chain header(String headerName, String headerValue, Handler handler);

  /**
   * Adds a {@code Handler} to this {@code Chain} that delegates to the given {@code Closure} as a {@code Handler} if the {@code request}
   * has a {@code HTTPHeader} with the given name and a it's value matches the given value exactly.
   * <p>
   * See {@link Chain#header(String, String, ratpack.handling.Handler)} for more details.
   *
   * @param headerName the name of the HTTP Header to match on
   * @param headerValue the value of the HTTP Header to match on
   * @param handler the handler to delegate to
   * @return this {@code Chain}
   */
  Chain header(String headerName, String headerValue, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);
}
