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

package org.ratpackframework.groovy.handling;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.util.Action;

import java.util.List;

/**
 * A Groovy oriented handler chain builder DSL.
 * <p>
 * The methods specific to this subclass create {@link org.ratpackframework.handling.Handler} instances from closures and
 * add them to the underlying chain.
 * <p>
 * These methods are generally shortcuts for {@link #handler(org.ratpackframework.handling.Handler)} on this underlying chain.
 */
public interface Chain extends org.ratpackframework.handling.Chain {

  /**
   * Adds a handler with the given closure as its implementation.
   *
   * @param handler The closure to add as a handler.
   */
  Chain handler(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  Chain handler(Handler handler);

  /**
   * Adds a nested chain that is inserted if the request path matches the given prefix.
   * <p>
   * All path based handlers become relative to the given prefix.
   * <pre class="groovy-chain-dsl">
   *   prefix("person/:id") {
   *    get("info") {
   *      // e.g. /person/2/info
   *    }
   *    post("save") {
   *      // e.g. /person/2/save
   *    }
   *    prefix("child/:childId") {
   *      get("info") {
   *        // e.g. /person/2/child/1/info
   *      }
   *    }
   *   }
   * </pre>
   * <p>
   * See {@link org.ratpackframework.handling.Handlers#prefix(String, org.ratpackframework.util.Action)} for format details on the prefix string.
   *
   * @param prefix The prefix to bind to.
   * @param chain The definition of the nested handlers
   */
  Chain prefix(String prefix, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> chain);

  Chain prefix(String prefix, Handler... handlers);

  /**
   * {@inheritDoc}
   */
  org.ratpackframework.handling.Chain prefix(String prefix, Handler handler);

  Chain path(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  Chain path(String path, Handler handler);

  /**
   * {@inheritDoc}
   */
  Chain get(Handler handler);

  /**
   * {@inheritDoc}
   */
  Chain get(String path, Handler handler);

  Chain get(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  Chain get(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  Chain post(String path, Handler handler);

  Chain post(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  /**
   * {@inheritDoc}
   */
  Chain post(Handler handler);

  Chain post(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  Chain register(Object object, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

  Chain register(Object object, List<Handler> handlers);

  <T> Chain register(Class<? super T> type, T object, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

  <T> Chain register(Class<? super T> type, T object, List<Handler> handlers);

  Chain fileSystem(String path, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

  Chain prefix(String prefix, List<Handler> handlers);

  Chain prefix(String prefix, Action<? super org.ratpackframework.handling.Chain> chainAction);

  @Override
  Chain assets(String path, String... indexFiles);

  @Override
  Chain fileSystem(String path, List<Handler> handlers);
}
