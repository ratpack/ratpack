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

import com.google.common.collect.ImmutableList;
import org.ratpackframework.context.Context;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

public class ContextInsertingHandler implements Handler {

  private final Class<?> type;
  private final Object object;
  private final ImmutableList<Handler> handlers;

  @SuppressWarnings("unchecked")
  public <T> ContextInsertingHandler(T object, ImmutableList<Handler> handlers) {
    this.type = null;
    this.object = object;
    this.handlers = handlers;
  }

  public <T> ContextInsertingHandler(Class<? super T> type, T object, ImmutableList<Handler> handlers) {
    this.type = type;
    this.object = object;
    this.handlers = handlers;
  }

  @SuppressWarnings("unchecked")
  public void handle(Exchange exchange) {
    Context context = exchange.getContext();
    if (type == null) {
      exchange.insert(context.plus(object), handlers);
    } else {
      exchange.insert(context.plus((Class<? super Object>) type, object), handlers);
    }
  }
}
