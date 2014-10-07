/*
 * Copyright 2014 the original author or authors.
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

package ratpack.reactor;

import ratpack.exec.ExecControl;
import ratpack.exec.Fulfiller;
import ratpack.exec.Promise;
import ratpack.func.Action;
import reactor.core.composable.Deferred;

public abstract class RatpackReactor {

  public static <T> reactor.core.composable.Promise<T> consume(final Fulfiller<? super T> fulfiller, reactor.core.composable.Promise<T> reactorPromise) {
    return reactorPromise.onSuccess(fulfiller::success).onError(fulfiller::error);
  }

  public static <T> Promise<T> consume(ExecControl execControl, final Deferred<T, ? extends reactor.core.composable.Promise<T>> deferred) {
    return consume(execControl, deferred.compose());
  }

  public static <T> Promise<T> consume(ExecControl execControl, final reactor.core.composable.Promise<T> reactorPromise) {
    return execControl.promise(new Action<Fulfiller<? super T>>() {
      @Override
      public void execute(Fulfiller<? super T> fulfiller) throws Exception {
        consume(fulfiller, reactorPromise);
      }
    });
  }

}
