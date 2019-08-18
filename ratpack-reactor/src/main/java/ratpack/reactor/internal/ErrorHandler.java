/*
 * Copyright 2018 the original author or authors.
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

package ratpack.reactor.internal;

import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.BiFunction;

public class ErrorHandler implements BiFunction<Throwable, Object, Throwable> {

  @Override
  public Throwable apply(Throwable throwable, Object o) {
    Throwable t;
    if (throwable instanceof UnsupportedOperationException) {
      t = throwable.getCause();
    } else if (throwable instanceof UndeclaredThrowableException) {
      t = ((UndeclaredThrowableException) throwable).getUndeclaredThrowable();
    } else {
      t = throwable;
    }
    if (Execution.isActive()) {
      Promise.error(t).then(Action.noop());
    }
    return t;
  }
}
