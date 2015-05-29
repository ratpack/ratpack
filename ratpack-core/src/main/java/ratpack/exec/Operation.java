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

package ratpack.exec;

import ratpack.api.NonBlocking;
import ratpack.exec.internal.DefaultOperation;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.func.Factory;

public interface Operation {

  static Operation of(Block block) {
    return ExecControl.execControl().operation(block);
  }

  Operation onError(Action<? super Throwable> onError);

  @NonBlocking
  void then(Block block);

  @NonBlocking
  default void then() {
    then(Block.noop());
  }

  Promise<Void> promise();

  default <T> Promise<T> map(Factory<? extends T> factory) {
    return promise().map(n -> factory.create());
  }

  default <T> Promise<T> flatMap(Factory<? extends Promise<T>> factory) {
    return promise().flatMap(n -> factory.create());
  }

  default <T> Promise<T> flatMap(Promise<T> promise) {
    return promise().flatMap(n -> promise);
  }

  default Operation next(Operation operation) {
    return new DefaultOperation(flatMap(operation::promise));
  }

  default Operation next(Block operation) {
    return next(ExecControl.execControl().operation(operation));
  }

}
