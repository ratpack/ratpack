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
import ratpack.groovy.Groovy;
import ratpack.handling.ByContentSpec;
import ratpack.handling.ByMethodSpec;
import ratpack.handling.Handler;

/**
 * A Groovy oriented content negotiation handler builder.
 *
 * @see GroovyContext#byMethod(Closure)
 * @see ByMethodSpec
 * @since 1.5
 */
public interface GroovyByContentSpec extends ByContentSpec {

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param handler the handler to invoke if the content type matches
   * @return this
   */
  default GroovyByContentSpec type(String mimeType, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return type(mimeType, Groovy.groovyHandler(handler));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.6
   */
  default GroovyByContentSpec type(CharSequence mimeType, @DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return type(mimeType, Groovy.groovyHandler(handler));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/plain".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   */
  default GroovyByContentSpec plainText(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return plainText(Groovy.groovyHandler(handler));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/html".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   */
  default GroovyByContentSpec html(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return html(Groovy.groovyHandler(handler));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/json".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   */
  default GroovyByContentSpec json(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return json(Groovy.groovyHandler(handler));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/xml".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   */
  default GroovyByContentSpec xml(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return xml(Groovy.groovyHandler(handler));
  }

  /**
   * Specifies that the given handler should be used if the client's requested content type cannot be matched with any of the other handlers.
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   */
  default GroovyByContentSpec noMatch(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return noMatch(Groovy.groovyHandler(handler));
  }

  /**
   * Specifies that the given handler should be used if the client did not provide a usable "Accept" header in the request.
   *
   * @param handler the handler to invoke if if no usable "Accept" header is present in the request.
   * @return this
   */
  default GroovyByContentSpec unspecified(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return unspecified(Groovy.groovyHandler(handler));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec type(String mimeType, Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec type(CharSequence mimeType, Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec type(String mimeType, Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec type(CharSequence mimeType, Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec type(String mimeType, Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec type(CharSequence mimeType, Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec plainText(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec plainText(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec plainText(Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec html(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec html(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec html(Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec json(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec json(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec json(Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec xml(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec xml(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec xml(Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec noMatch(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec noMatch(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec noMatch(Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec noMatch(String mimeType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec unspecified(Block block);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec unspecified(Handler handler);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec unspecified(Class<? extends Handler> handlerType);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyByContentSpec unspecified(String mimeType);

}
