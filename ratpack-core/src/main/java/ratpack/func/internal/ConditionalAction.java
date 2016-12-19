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

import ratpack.func.Action;
import ratpack.func.Predicate;

public class ConditionalAction<I> implements Action<I> {

  public static class Branch<I> {
    final Predicate<? super I> predicate;
    final Action<? super I> action;

    public Branch(Predicate<? super I> predicate, Action<? super I> action) {
      this.predicate = predicate;
      this.action = action;
    }
  }

  private final Iterable<? extends Branch<I>> branches;
  private final Action<? super I> other;

  public ConditionalAction(Iterable<? extends Branch<I>> branches, Action<? super I> other) {
    this.branches = branches;
    this.other = other;
  }

  @Override
  public final void execute(I i) throws Exception {
    for (Branch<I> branch : branches) {
      if (branch.predicate.apply(i)) {
        branch.action.execute(i);
        return;
      }
    }

    other.execute(i);
  }
}
