/*
 * Copyright 2015 the original author or authors.
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

import io.netty.util.concurrent.FastThreadLocal;
import ratpack.path.PathBinding;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class PathBindingStorage {

  private static final FastThreadLocal<Deque<PathBinding>> STACK = new FastThreadLocal<>();

  public static Deque<PathBinding> install(PathBinding pathBinding) {
    Deque<PathBinding> pathBindings = STACK.get();
    if (pathBindings == null) {
      pathBindings = new ArrayDeque<>(4);
      STACK.set(pathBindings);
    }
    pathBindings.clear();
    pathBindings.add(pathBinding);
    return pathBindings;
  }

  public static void push(PathBinding pathBinding) {
    STACK.get().push(pathBinding);
  }

  public static PathBinding get() {
    return STACK.get().peek();
  }

  public static void pop() {
    STACK.get().poll();
  }

}
