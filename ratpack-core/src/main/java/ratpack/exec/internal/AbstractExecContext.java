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

import ratpack.exec.ExecContext;
import ratpack.exec.ExecController;
import ratpack.exec.Fulfiller;
import ratpack.exec.Promise;
import ratpack.func.Action;

import java.util.concurrent.Callable;

public abstract class AbstractExecContext implements ExecContext {

  @Override
  public ExecContext getContext() {
    return this;
  }

  @Override
  public ExecController getExecController() {
    return getLaunchConfig().getExecController();
  }

  @Override
  public <T> Promise<T> blocking(Callable<T> blockingOperation) {
    return getExecController().getControl().blocking(blockingOperation);
  }

  @Override
  public <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return getExecController().getControl().promise(action);
  }

}
