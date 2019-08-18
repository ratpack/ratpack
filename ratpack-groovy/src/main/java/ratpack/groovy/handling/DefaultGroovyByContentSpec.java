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

import ratpack.func.Block;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Handler;

public class DefaultGroovyByContentSpec implements GroovyByContentSpec {

  private final ByContentSpec delegate;

  public DefaultGroovyByContentSpec(ByContentSpec delegate) {
    this.delegate = delegate;
  }

  private GroovyByContentSpec r(Object ignore) {
    return this;
  }

  @Override
  public GroovyByContentSpec type(String mimeType, Block block) {
    return r(delegate.type(mimeType, block));
  }

  @Override
  public GroovyByContentSpec type(CharSequence mimeType, Block block) {
    return r(delegate.type(mimeType, block));
  }

  @Override
  public GroovyByContentSpec type(String mimeType, Handler handler) {
    return r(delegate.type(mimeType, handler));
  }

  @Override
  public GroovyByContentSpec type(CharSequence mimeType, Handler handler) {
    return r(delegate.type(mimeType, handler));
  }

  @Override
  public GroovyByContentSpec type(String mimeType, Class<? extends Handler> handlerType) {
    return r(delegate.type(mimeType, handlerType));
  }

  @Override
  public GroovyByContentSpec type(CharSequence mimeType, Class<? extends Handler> handlerType) {
    return r(delegate.type(mimeType, handlerType));
  }

  @Override
  public GroovyByContentSpec plainText(Block block) {
    return r(delegate.plainText(block));
  }

  @Override
  public GroovyByContentSpec plainText(Handler handler) {
    return r(delegate.plainText(handler));
  }

  @Override
  public GroovyByContentSpec plainText(Class<? extends Handler> handlerType) {
    return r(delegate.plainText(handlerType));
  }

  @Override
  public GroovyByContentSpec html(Block block) {
    return r(delegate.html(block));
  }

  @Override
  public GroovyByContentSpec html(Handler handler) {
    return r(delegate.html(handler));
  }

  @Override
  public GroovyByContentSpec html(Class<? extends Handler> handlerType) {
    return r(delegate.html(handlerType));
  }

  @Override
  public GroovyByContentSpec json(Block block) {
    return r(delegate.json(block));
  }

  @Override
  public GroovyByContentSpec json(Handler handler) {
    return r(delegate.json(handler));
  }

  @Override
  public GroovyByContentSpec json(Class<? extends Handler> handlerType) {
    return r(delegate.json(handlerType));
  }

  @Override
  public GroovyByContentSpec xml(Block block) {
    return r(delegate.xml(block));
  }

  @Override
  public GroovyByContentSpec xml(Handler handler) {
    return r(delegate.xml(handler));
  }

  @Override
  public GroovyByContentSpec xml(Class<? extends Handler> handlerType) {
    return r(delegate.xml(handlerType));
  }

  @Override
  public GroovyByContentSpec noMatch(Block block) {
    return r(delegate.noMatch(block));
  }

  @Override
  public GroovyByContentSpec noMatch(Handler handler) {
    return r(delegate.noMatch(handler));
  }

  @Override
  public GroovyByContentSpec noMatch(Class<? extends Handler> handlerType) {
    return r(delegate.noMatch(handlerType));
  }

  @Override
  public GroovyByContentSpec noMatch(String mimeType) {
    return r(delegate.noMatch(mimeType));
  }

  @Override
  public GroovyByContentSpec unspecified(Block block) {
    return r(delegate.unspecified(block));
  }

  @Override
  public GroovyByContentSpec unspecified(Handler handler) {
    return r(delegate.unspecified(handler));
  }

  @Override
  public GroovyByContentSpec unspecified(Class<? extends Handler> handlerType) {
    return r(delegate.unspecified(handlerType));
  }

  @Override
  public GroovyByContentSpec unspecified(String mimeType) {
    return r(delegate.unspecified(mimeType));
  }

}
