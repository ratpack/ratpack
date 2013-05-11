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

package org.ratpackframework.context;

public abstract class ContextSupport implements Context {

  private final Context parent;

  protected ContextSupport(Context parent) {
    this.parent = parent;
  }

  public abstract <T> T doMaybeGet(Class<T> type);

  @Override
  public <T> T maybeGet(Class<T> type) {
    T value = doMaybeGet(type);

    if (value == null) {
      return parent.maybeGet(type);
    } else {
      return value;
    }
  }

  @Override
  public <T> T get(Class<T> type) {
    T found = maybeGet(type);
    if (found == null) {
      throw new IllegalStateException(String.format("Could not find %s in context", type));
    }

    return found;
  }

}
