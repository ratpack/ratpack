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

package org.ratpackframework.routing;

import org.ratpackframework.Action;

import java.util.ArrayList;
import java.util.List;

public class CompositeRouter<T> implements Action<Routed<T>> {

  List<Action<Routed<T>>> handlers;

  public CompositeRouter(List<Action<Routed<T>>> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void execute(Routed<T> thing) {
    next(new ArrayList<>(handlers), thing, thing);
  }

  private void next(final List<Action<Routed<T>>> remaining, final Routed<T> previous, final Routed<T> original) {
    if (remaining.isEmpty()) {
      original.next();
    } else {
      Action<Routed<T>> router = remaining.remove(0);
      Routed<T> forwarding = previous.withNext(new Action<Routed<T>>() {
        @Override
        public void execute(Routed<T> event) {
          next(remaining, event, original);
        }
      });
      router.execute(forwarding);
    }
  }

}
