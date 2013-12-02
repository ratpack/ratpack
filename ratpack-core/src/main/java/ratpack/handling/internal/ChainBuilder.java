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

package ratpack.handling.internal;

import com.google.common.collect.ImmutableList;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.util.Action;
import ratpack.util.Transformer;

import java.util.LinkedList;
import java.util.List;

public class ChainBuilder {

  public static final ChainBuilder INSTANCE = new ChainBuilder();

  public <T> Handler buildHandler(Transformer<List<Handler>, ? extends T> transformer, Action<? super T> action) {
    return Handlers.chain(buildList(transformer, action));
  }

  public <T> List<? extends Handler> buildList(Transformer<List<Handler>, ? extends T> transformer, Action<? super T> action) {
    List<Handler> handlers = new LinkedList<>();
    T thing = transformer.transform(handlers);
    try {
      action.execute(thing);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return ImmutableList.copyOf(handlers);
  }

}
