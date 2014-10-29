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

package ratpack.path.internal;

import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;
import ratpack.registry.Registries;

public class PathHandler implements Handler {

  private final PathBinder binding;
  private final Handler handler;

  public PathHandler(PathBinder binding, Handler handler) {
    this.binding = binding;
    this.handler = handler;
  }

  public void handle(Context context) {
    PathBinding childBinding = binding.bind(context.getRequest().getPath(), context.maybeGet(PathBinding.class).orElse(null));
    if (childBinding != null) {
      context.insert(Registries.just(PathBinding.class, childBinding), handler);
    } else {
      context.next();
    }
  }
}
