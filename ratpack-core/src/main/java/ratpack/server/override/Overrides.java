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

package ratpack.server.override;

import com.google.common.reflect.TypeToken;
import ratpack.func.Factory;
import ratpack.registry.Registry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class Overrides implements Registry {

  private static final ThreadLocal<Deque<Registry>> OVERRIDES = ThreadLocal.withInitial(ArrayDeque::new);

  private final Registry delegate;

  private Overrides(Registry delegate) {
    this.delegate = delegate;
  }

  public static <T> T setFor(Registry overrides, Factory<? extends T> during) throws Exception {
    OVERRIDES.get().push(overrides);
    try {
      return during.create();
    } finally {
      OVERRIDES.get().poll();
    }
  }

  public static Overrides none() {
    return new Overrides(Registry.empty());
  }

  public static Overrides get() {
    return new Overrides(Optional.ofNullable(OVERRIDES.get()).map(Deque::poll).orElse(Registry.empty()));
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return delegate.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return delegate.getAll(type);
  }
}
