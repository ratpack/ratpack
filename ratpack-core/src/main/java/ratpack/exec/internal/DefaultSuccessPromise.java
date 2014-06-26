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

package ratpack.exec.internal;

import ratpack.exec.Fulfiller;
import ratpack.exec.OverlappingExecutionException;
import ratpack.exec.SuccessPromise;
import ratpack.exec.internal.DefaultExecController.Execution;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.util.ExceptionUtils;
import ratpack.util.internal.InternalRatpackError;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final Factory<Execution> executionFactory;
  private final Action<? super Fulfiller<T>> action;
  private final Action<? super Throwable> errorHandler;
  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultSuccessPromise.class);

  public DefaultSuccessPromise(Factory<Execution> executionFactory, Action<? super Fulfiller<T>> action, Action<? super Throwable> errorHandler) {
    this.executionFactory = executionFactory;
    this.action = action;
    this.errorHandler = errorHandler;
  }

  @Override
  public void then(final Action<? super T> then) {
    try {
      final Execution execution = executionFactory.create();
      execution.continueVia(new Runnable() {

        private final AtomicBoolean fulfilled = new AtomicBoolean();

        @Override
        public void run() {
          try {
            action.execute(new Fulfiller<T>() {
              @Override
              public void error(final Throwable throwable) {
                if (!fulfilled.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
                  return;
                }

                execution.join(new Action<ratpack.exec.Execution>() {
                  @Override
                  public void execute(ratpack.exec.Execution execution) throws Exception {
                    errorHandler.execute(throwable);
                  }
                });
              }

              @Override
              public void success(final T value) {
                if (!fulfilled.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
                  return;
                }

                execution.join(new Action<ratpack.exec.Execution>() {
                  @Override
                  public void execute(ratpack.exec.Execution execution) throws Exception {
                    then.execute(value);
                  }
                });
              }
            });
          } catch (final Exception e) {
            if (!fulfilled.compareAndSet(false, true)) {
              LOGGER.error("", new OverlappingExecutionException("exception thrown after promise was fulfilled", e));
            } else {
              execution.join(new Action<ratpack.exec.Execution>() {
                @Override
                public void execute(ratpack.exec.Execution execution) throws Exception {
                  throw ExceptionUtils.toException(e);
                }
              });
            }
          }
        }
      });
    } catch (Exception e) {
      throw new InternalRatpackError("failed to add promise resume action");
    }
  }

}
