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

package org.ratpackframework.context.internal;

import org.ratpackframework.context.Context;
import org.ratpackframework.context.NotInContextException;

public abstract class ContextSupport implements Context {

  protected abstract <T> T doMaybeGet(Class<T> type);

  protected <T> T onNotFound(Class<T> type) {
    return null;
  }

  public final <T> T maybeGet(Class<T> type) {
    T value = doMaybeGet(type);

    if (value == null) {
      return onNotFound(type);
    } else {
      return value;
    }
  }

  public final <T> T get(Class<T> type) {
    T found = maybeGet(type);
    if (found == null) {
      throw new NotInContextException(this, type);
    }

    return found;
  }

}
