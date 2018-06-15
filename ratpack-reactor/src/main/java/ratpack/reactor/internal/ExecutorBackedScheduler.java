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

import ratpack.exec.ExecController;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ExecutorBackedScheduler implements Scheduler {

  protected final ExecController execController;
  protected final Scheduler delegateScheduler;

  protected ExecutorBackedScheduler(ExecController execController) {
    this.execController = execController;
    this.delegateScheduler = Schedulers.fromExecutor(getExecutor());
  }

  abstract ExecutorService getExecutor();

  @Override
  public long now(@NonNull TimeUnit unit) {
    return delegateScheduler.now(unit);
  }

  @Override
  public Disposable schedule(Runnable runnable) {
    return delegateScheduler.schedule(runnable);
  }

  @Override
  public Worker createWorker() {
    return new Worker() {
      private final Worker delegateWorker = delegateScheduler.createWorker();

      class ExecutionWrappedAction implements Runnable {
        private final Runnable delegate;

        ExecutionWrappedAction(Runnable delegate) {
          this.delegate = delegate;
        }

        @Override
        public void run() {
          execController.fork().start(execution -> delegate.run());
        }
      }

      @Override
      public Disposable schedule(@NonNull Runnable run) {
        return delegateWorker.schedule(new ExecutionWrappedAction(run));
      }

      @Override
      public Disposable schedule(@NonNull Runnable run, long delayTime, TimeUnit unit) {
        return delegateWorker.schedule(new ExecutionWrappedAction(run), delayTime, unit);
      }

      @Override
      public void dispose() {
        delegateWorker.dispose();
      }

      @Override
      public boolean isDisposed() {
        return delegateWorker.isDisposed();
      }

    };
  }
}
