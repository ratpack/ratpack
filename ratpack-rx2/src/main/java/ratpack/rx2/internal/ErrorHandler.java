/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rx2.internal;

import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Consumer;
import ratpack.exec.Promise;
import ratpack.func.Action;

public class ErrorHandler implements Consumer<Throwable> {
  @Override
  public void accept(Throwable e) throws Exception {
    if (e instanceof OnErrorNotImplementedException) {
      Promise.error(e.getCause()).then(Action.noop());
    } else if (e instanceof UndeliverableException) {
      Promise.error(e.getCause()).then(Action.noop());
    } else {
      Promise.error(e).then(Action.noop());
    }
  }
}
