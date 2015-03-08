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

package ratpack.test.exec.internal;

import ratpack.exec.*;
import ratpack.exec.internal.CompleteExecResult;
import ratpack.exec.internal.ResultBackedExecResult;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.test.exec.ExecHarness;
import ratpack.exec.ExecResult;
import ratpack.util.ExceptionUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultExecHarness implements ExecHarness {

  private final ExecController controller;

  public DefaultExecHarness(ExecController controller) {
    this.controller = controller;
  }

  @Override
  public <T> ExecResult<T> yield(final Function<ExecControl, Promise<T>> func) throws Exception {
    final AtomicReference<ExecResult<T>> reference = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    controller.getControl().exec()
      .onError(throwable -> {
        reference.set(new ResultBackedExecResult<>(Result.<T>failure(throwable)));
        latch.countDown();
      })
      .start(new Action<Execution>() {
        @Override
        public void execute(Execution execution) throws Exception {
          execution.onCleanup(() -> {
            if (latch.getCount() > 0) {
              reference.set(CompleteExecResult.instance());
              latch.countDown();
            }
          });
          Promise<T> promise = func.apply(execution.getControl());

          if (promise == null) {
            reference.set(null);
            latch.countDown();
          } else {
            promise.then(t -> {
              reference.set(new ResultBackedExecResult<>(Result.success(t)));
              latch.countDown();
            });
          }
        }
      });
    latch.await();
    return reference.get();
  }

  @Override
  public void run(Action<? super ExecControl> action) throws Exception {
    final AtomicReference<Throwable> thrown = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    controller.getControl().exec()
      .onError(thrown::set)
      .onComplete(e ->
          latch.countDown()
      )
      .start(e ->
          action.execute(e.getControl())
      );

    latch.await();

    Throwable throwable = thrown.get();
    if (throwable != null) {
      throw ExceptionUtils.toException(throwable);
    }
  }

  @Override
  public ExecControl getControl() {
    return controller.getControl();
  }

  @Override
  public void close() {
    controller.close();
  }

}
