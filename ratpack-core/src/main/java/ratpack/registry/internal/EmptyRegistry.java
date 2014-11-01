/*
 * Copyright 2014 the original author or authors.
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

package ratpack.registry.internal;

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.registry.Registry;

import java.util.Collections;
import java.util.Optional;

public final class EmptyRegistry implements Registry {

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return Optional.empty();
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return Collections.emptyList();
  }

  @Override
  public <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate) {
    return Optional.empty();
  }

  @Override
  public <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate) {
    return Collections.emptyList();
  }

  @Override
  public <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception {
    return false;
  }
}
