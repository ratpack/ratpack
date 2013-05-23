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

package org.ratpackframework.handling.internal;

import org.ratpackframework.context.Context;
import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.error.internal.ErrorCatchingHandler;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

public class ContextInsertingHandler implements Handler {

  private final Class<?> type;
  private final Object object;
  private final Handler handler;

  @SuppressWarnings("unchecked")
  public <T> ContextInsertingHandler(T object, Handler handler) {
    this.type = null;
    this.object = object;
    this.handler = decorate((Class<? super T>) object.getClass(), handler);
  }

  public <T> ContextInsertingHandler(Class<? super T> type, T object, Handler handler) {
    this.type = type;
    this.object = object;
    this.handler = decorate(type, handler);
  }

  protected <T> Handler decorate(Class<? super T> type, Handler handler) {
    if (ServerErrorHandler.class.isAssignableFrom(type)) {
      return new ErrorCatchingHandler(handler);
    } else {
      return handler;
    }
  }

  @SuppressWarnings("unchecked")
  public void handle(Exchange exchange) {
    Context context = exchange.getContext();
    if (type == null) {
      exchange.insert(context.plus(object), handler);
    } else {
      exchange.insert(context.plus((Class<? super Object>) type, object), handler);
    }
  }
}
