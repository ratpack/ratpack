/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.handler.internal;

import org.ratpackframework.handler.Handler;

import java.util.ArrayList;
import java.util.List;

public class CompositeHandler<T> implements Handler<T> {

  private final ExhaustionHandler<T> exhausted;
  private final NextFactory<T> nextFactory;
  List<Handler<T>> handlers;

  public static interface ExhaustionHandler<T> {
    void exhausted(T original, T last);
  }

  public static interface NextFactory<T> {
    T makeNext(T original, Handler<? super T> next);
  }

  public CompositeHandler(List<Handler<T>> handlers, ExhaustionHandler<T> exhausted, NextFactory<T> nextFactory) {
    this.handlers = handlers;
    this.exhausted = exhausted;
    this.nextFactory = nextFactory;
  }

  @Override
  public void handle(T thing) {
    next(new ArrayList<>(handlers), thing, thing);
  }

  private void next(final List<Handler<T>> remaining, final T previous, final T original) {
    if (remaining.isEmpty()) {
      exhausted.exhausted(original, previous);
    } else {
      Handler<? super T> router = remaining.remove(0);
      router.handle(nextFactory.makeNext(previous, new Handler<T>() {
        @Override
        public void handle(T event) {
          next(remaining, event, original);
        }
      }));
    }
  }

}
