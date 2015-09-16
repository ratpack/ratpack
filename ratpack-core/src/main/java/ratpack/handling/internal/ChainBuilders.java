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

import com.google.common.collect.Lists;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;

import java.util.List;

public class ChainBuilders {

  public static <T> Handler build(final Function<List<Handler>, ? extends T> toChainBuilder, final Action<? super T> chainBuilderAction) throws Exception {
    List<Handler> handlers = Lists.newLinkedList();
    T chainBuilder = toChainBuilder.apply(handlers);
    chainBuilderAction.execute(chainBuilder);
    return Handlers.chain(handlers.toArray(new Handler[handlers.size()]));
  }

}
