/*
 * Copyright 2022 the original author or authors.
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

package ratpack.func.internal;

import ratpack.func.Action;

public class NoopAction<T> implements Action<T> {

  public static final Action<Object> INSTANCE = new NoopAction<>();

  private NoopAction() {
  }

  @Override
  public void execute(T t) throws Exception {

  }

  @Override
  @SuppressWarnings("unchecked")
  public <O extends T> Action<O> append(Action<? super O> action) {
    return (Action<O>) action;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <O extends T> Action<O> prepend(Action<? super O> action) {
    return (Action<O>) action;
  }
}
