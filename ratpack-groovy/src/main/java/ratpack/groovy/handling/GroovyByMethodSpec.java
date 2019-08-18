/*
 * Copyright 2017 the original author or authors.
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
import ratpack.func.Block;
import ratpack.handling.ByMethodSpec;
import ratpack.handling.Handler;

/**
 * A Groovy oriented multi-method handler builder.
 *
 * @see GroovyContext#byMethod(Closure)
 * @see ByMethodSpec
 * @since 1.5
 */
public interface GroovyByMethodSpec extends ByMethodSpec {

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec get(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec get(Class<? extends Handler> clazz);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec get(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec post(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec post(Class<? extends Handler> clazz);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec post(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec put(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec put(Class<? extends Handler> clazz);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec put(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec patch(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec patch(Class<? extends Handler> clazz);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec patch(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec options(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec options(Class<? extends Handler> clazz);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec options(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec delete(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec delete(Class<? extends Handler> clazz);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec delete(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByMethodSpec named(String methodName, Block block);

  @Override
  GroovyByMethodSpec named(String methodName, Class<? extends Handler> clazz);

  @Override
  GroovyByMethodSpec named(String methodName, Handler handler);

  /**
   * Inserts the handler to chain if the request has a HTTP method of GET.
   *
   * @param closure a handler closure
   * @return this
   */
  GroovyByMethodSpec get(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of POST.
   *
   * @param closure a handler closure
   * @return this
   */
  GroovyByMethodSpec post(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PUT.
   *
   * @param closure a handler closure
   * @return this
   */
  GroovyByMethodSpec put(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PATCH.
   *
   * @param closure a handler closure
   * @return this
   */
  GroovyByMethodSpec patch(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of OPTIONS.
   *
   * @param closure a handler closure
   * @return this
   */
  GroovyByMethodSpec options(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of DELETE.
   *
   * @param closure a handler closure
   * @return this
   */
  GroovyByMethodSpec delete(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of {@code methodName}.
   * @param closure a handler closure
   * @return this
   */
  GroovyByMethodSpec named(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) String methodName, Closure<?> closure);

}
