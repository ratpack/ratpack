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
import org.ratpackframework.handling.Handler;
import org.ratpackframework.util.Action;
import org.ratpackframework.util.internal.Transformer;

import java.util.LinkedList;
import java.util.List;

public class ChainBuilder  {

  public static final ChainBuilder INSTANCE = new ChainBuilder();

  public <T> Handler build(Transformer<List<Handler>, ? extends T> transformer, Action<? super T> action) {
    List<Handler> handlers = new LinkedList<Handler>();
    T thing = transformer.transform(handlers);
    action.execute(thing);
    return new ChainHandler(ImmutableList.copyOf(handlers));
  }

}
