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

package ratpack.guice.internal;

import com.google.inject.Key;

import java.util.HashMap;

public class ExecutionScope extends ExecutionBasedScope<ExecutionScope.Store> {

  static class Store extends HashMap<Key<?>, Object> {}

  public ExecutionScope() {
    super(Store.class, "an execution");
  }

  @Override
  protected Store createStore() {
    return new Store();
  }
}
