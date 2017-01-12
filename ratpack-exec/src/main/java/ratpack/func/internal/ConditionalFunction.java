/*
 * Copyright 2016 the original author or authors.
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

package ratpack.func.internal;

import ratpack.func.Function;
import ratpack.func.Predicate;

public class ConditionalFunction<I, O> implements Function<I, O> {

  public static class Branch<I, O> {
    final Predicate<? super I> predicate;
    final Function<? super I, ? extends O> function;

    public Branch(Predicate<? super I> predicate, Function<? super I, ? extends O> function) {
      this.predicate = predicate;
      this.function = function;
    }
  }

  private final Iterable<? extends Branch<I, O>> branches;
  private final Function<? super I, ? extends O> other;

  public ConditionalFunction(Iterable<? extends Branch<I, O>> branches, Function<? super I, ? extends O> other) {
    this.branches = branches;
    this.other = other;
  }

  @Override
  public final O apply(I i) throws Exception {
    for (Branch<I, O> branch : branches) {
      if (branch.predicate.apply(i)) {
        return branch.function.apply(i);
      }
    }

    return other.apply(i);
  }
}
