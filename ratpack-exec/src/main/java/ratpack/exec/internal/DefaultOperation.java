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

import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.func.Predicate;

public class DefaultOperation implements Operation {

  private final Upstream<?> upstream;

  public DefaultOperation(Upstream<?> upstream) {
    this.upstream = upstream;
  }

  public DefaultOperation(Promise<?> promise) {
    this(downstream -> promise.connect(new Downstream<Object>() {
      @Override
      public void success(Object value) {
        downstream.success(null);
      }

      @Override
      public void error(Throwable throwable) {
        downstream.error(throwable);
      }

      @Override
      public void complete() {
        downstream.complete();
      }
    }));
  }

  @Override
  public Promise<Void> promise() {
    return Promise.async(down ->
      upstream.connect(new Downstream<Object>() {
        @Override
        public void success(Object value) {
          down.success(null);
        }

        @Override
        public void error(Throwable throwable) {
          down.error(throwable);
        }

        @Override
        public void complete() {
          down.complete();
        }
      })
    );
  }

  @Override
  public Operation onError(Predicate<? super Throwable> predicate, Action<? super Throwable> errorHandler) {
    return new DefaultOperation(down ->
      upstream.connect(new Downstream<Object>() {
        @Override
        public void success(Object value) {
          down.success(null);
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        public void error(Throwable throwable) {
          try {
            if (predicate.apply(throwable)) {
              Promise.<Void>sync(() -> {
                  errorHandler.execute(throwable);
                  return null;
                })
                .connect(new Downstream<Void>() {
                  @Override
                  public void success(Void value) {
                    down.complete();
                  }

                  @Override
                  public void error(Throwable e) {
                    down.error(e);
                  }

                  @Override
                  public void complete() {
                    down.complete();
                  }
                });
            } else {
              down.error(throwable);
            }
          } catch (Throwable e) {
            down.error(e);
          }
        }

        @Override
        public void complete() {
          down.complete();
        }
      })
    );
  }

  @Override
  public void then(Block block) {
    try {
      upstream.connect(new Downstream<Object>() {
        @Override
        public void success(Object value) {
          try {
            block.execute();
          } catch (Throwable e) {
            DefaultPromise.throwError(e);
          }
        }

        @Override
        public void error(Throwable throwable) {
          DefaultPromise.throwError(throwable);
        }

        @Override
        public void complete() {

        }
      });
    } catch (ExecutionException e) {
      throw e;
    } catch (Throwable e) {
      DefaultPromise.throwError(e);
    }
  }

}
