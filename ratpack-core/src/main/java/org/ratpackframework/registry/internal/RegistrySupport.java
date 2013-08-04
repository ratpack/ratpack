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

import org.ratpackframework.registry.NotInRegistryException;
import org.ratpackframework.registry.Registry;

public abstract class RegistrySupport<T> implements Registry<T> {

  protected abstract <O extends T> O doMaybeGet(Class<O> type);

  protected <O extends T> O onNotFound(@SuppressWarnings("UnusedParameters") Class<O> type) {
    return null;
  }

  public final <O extends T> O maybeGet(Class<O> type) {
    O value = doMaybeGet(type);

    if (value == null) {
      return onNotFound(type);
    } else {
      return value;
    }
  }

  public final <O extends T> O get(Class<O> type) {
    O found = maybeGet(type);
    if (found == null) {
      throw new NotInRegistryException(this, type);
    }

    return found;
  }

}
