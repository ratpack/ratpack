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

package org.ratpackframework.registry.internal;

import org.ratpackframework.registry.Registry;
import org.ratpackframework.util.Factory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LazyChildRegistry<T, L extends T> extends SingleValueChildRegistry<T, L> {

  private final Factory<? extends L> factory;
  private L object;
  private Lock lock = new ReentrantLock();

  public LazyChildRegistry(Registry<T> parent, Class<L> type, Factory<? extends L> factory) {
    super(parent, type);
    this.factory = factory;
  }

  protected L getObject() {
    if (object == null) {
      lock.lock();
      try {
        //noinspection ConstantConditions
        if (object == null) {
          object = factory.create();
        }
      } finally {
        lock.unlock();
      }
    }
    return object;
  }

  @Override
  protected String describe() {
    return "LazyChildRegistry{" + getType().getName() + "}";
  }
}
