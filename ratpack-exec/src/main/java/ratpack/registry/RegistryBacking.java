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

package ratpack.registry;

import com.google.common.base.Supplier;
import com.google.common.reflect.TypeToken;

/**
 * Provides instances to the Registry implementation which uses an implementation of
 * this interface for backing the instances that the Registry contains or returns.
 *
 * The return type of the provide method is an iterable result of Supplier instances.
 * The actual instances won't be cached in the Registry implementation and
 * the supplier will be called each time an instance is requested. This allows more
 * flexibility over the lifecycle of the instances that Registry returns.
 *
 */
public interface RegistryBacking {
  /**
   * Provides instances to the Registry implementation which uses this method for looking up an iterable
   * result of Supplier instances for the given type.
   *
   * @param type the type to look up
   * @param <T> the class of the type to lookup
   * @return an iterable result of Supplier instances that return instances for the given type to look up
   */
  <T> Iterable<Supplier<? extends T>> provide(TypeToken<T> type);
}
