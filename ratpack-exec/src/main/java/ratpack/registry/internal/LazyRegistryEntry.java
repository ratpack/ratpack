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

package ratpack.registry.internal;

import com.google.common.reflect.TypeToken;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class LazyRegistryEntry<T> implements RegistryEntry<T> {

  private final TypeToken<T> type;
  private final Supplier<? extends T> supplier;
  private final Lock lock = new ReentrantLock();

  private T object;

  public LazyRegistryEntry(TypeToken<T> type, Supplier<? extends T> supplier) {
    this.type = type;
    this.supplier = supplier;
  }

  @Override
  public TypeToken<T> getType() {
    return type;
  }

  @Override
  public T get() {
    if (object == null) {
      lock.lock();
      try {
        //noinspection ConstantConditions
        if (object == null) {
          object = supplier.get();
        }
      } finally {
        lock.unlock();
      }
    }
    return object;
  }

  @Override
  public String toString() {
    return "LazyRegistryEntry{type=" + type + ", supplier=" + supplier + ", value=" + object + '}';
  }
}
