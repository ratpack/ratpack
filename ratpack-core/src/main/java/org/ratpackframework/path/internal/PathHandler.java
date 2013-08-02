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

package org.ratpackframework.path.internal;

import com.google.common.collect.ImmutableList;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.path.PathBinder;
import org.ratpackframework.path.PathBinding;

import java.util.List;

public class PathHandler implements Handler {

  private final PathBinder binding;
  private final List<Handler> chain;

  public PathHandler(PathBinder binding, ImmutableList<Handler> chain) {
    this.binding = binding;
    this.chain = chain;
  }

  public void handle(Context context) {
    PathBinding childBinding = binding.bind(context.getRequest().getPath(), context.maybeGet(PathBinding.class));
    if (childBinding != null) {
      context.insert(PathBinding.class, childBinding, chain);
    } else {
      context.next();
    }
  }
}
