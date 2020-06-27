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

package ratpack.exec.internal;

import ratpack.exec.Downstream;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.Block;

public class DefaultOperation implements Operation {

  private final Promise<Void> promise;

  public DefaultOperation(Promise<Void> promise) {
    this.promise = promise;
  }

  @Override
  public Promise<Void> promise() {
    return promise;
  }

  @Override
  public Operation onError(Action<? super Throwable> onError) {
    return new DefaultOperation(
      promise.transform(up -> down ->
        up.connect(new Downstream<Void>() {
          @Override
          public void success(Void value) {
            down.success(value);
          }

          @Override
          public void error(Throwable throwable) {
            Operation.of(() -> onError.execute(throwable)).promise().connect(new Downstream<Void>() {
              @Override
              public void success(Void value) {
                down.complete();
              }

              @Override
              public void error(Throwable throwable) {
                down.error(throwable);
              }

              @Override
              public void complete() {
                down.complete();
              }
            });
          }

          @Override
          public void complete() {
            down.complete();
          }
        })
      )
    );
  }

  @Override
  public void then(Block block) {
    promise.then(v ->
      block.execute()
    );
  }

}
