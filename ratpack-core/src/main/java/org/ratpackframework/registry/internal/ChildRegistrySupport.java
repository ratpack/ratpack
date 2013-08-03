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

public abstract class ChildRegistrySupport extends RegistrySupport {

  private final Registry parent;

  protected ChildRegistrySupport(Registry parent) {
    this.parent = parent;
  }

  @Override
  public String toString() {
    return describe() + " -> " + parent.toString();
  }

  @Override
  public <T> T onNotFound(Class<T> type) {
    return parent.maybeGet(type);
  }

  protected abstract String describe();

}
