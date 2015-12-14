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
import ratpack.exec.internal.ResultBackedExecResult;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.registry.RegistrySpec;
import ratpack.test.exec.ExecHarness;
import ratpack.util.Exceptions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultExecHarness implements ExecHarness {

  private final ExecController controller;

  public DefaultExecHarness(ExecController controller) {
    this.controller = controller;
  }

  @Override
  public ExecController getController() {
    return controller;
  }

  @Override
  public <T> ExecResult<T> yield(Action<? super RegistrySpec> registry, final Function<? super Execution, ? extends Promise<T>> func) throws Exception {
    AtomicReference<ExecResult<T>> reference = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    controller.fork()
      .register(registry)
      .onError(throwable -> reference.set(new ResultBackedExecResult<>(Result.<T>error(throwable))))
      .onComplete(exec -> latch.countDown())
      .start(execution -> {
        reference.set(ExecResult.complete());
        Promise<T> promise = func.apply(execution);
        if (promise == null) {
          reference.set(null);
        } else {
          promise.then(t -> reference.set(new ResultBackedExecResult<>(Result.success(t))));
        }
      });
    latch.await();
    return reference.get();
  }

  @Override
  public void run(Action<? super RegistrySpec> registry, Action<? super Execution> action) throws Exception {
    final AtomicReference<Throwable> thrown = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    controller.fork()
      .onError(thrown::set)
      .register(registry)
      .onComplete(e ->
        latch.countDown()
      )
      .start(action::execute);

    latch.await();

    Throwable throwable = thrown.get();
    if (throwable != null) {
      throw Exceptions.toException(throwable);
    }
  }

  @Override
  public void close() {
    controller.close();
  }

}
