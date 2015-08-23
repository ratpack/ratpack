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

package ratpack.rx.internal;

import ratpack.exec.ExecController;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.concurrent.TimeUnit;

public class ExecControllerBackedScheduler extends Scheduler {

  private final ExecController execController;
  private final Scheduler delegateScheduler;

  public ExecControllerBackedScheduler(ExecController execController) {
    this.execController = execController;
    this.delegateScheduler = Schedulers.from(execController.getEventLoopGroup());
  }

  @Override
  public long now() {
    return delegateScheduler.now();
  }

  @Override
  public Worker createWorker() {
    return new Worker() {
      private final Worker delegateWorker = delegateScheduler.createWorker();

      class ExecutionWrappedAction implements Action0 {
        private final Action0 delegate;

        ExecutionWrappedAction(Action0 delegate) {
          this.delegate = delegate;
        }

        @Override
        public void call() {
          execController.fork().start(execution -> delegate.call());
        }
      }

      @Override
      public Subscription schedule(final Action0 action) {
        return delegateWorker.schedule(new ExecutionWrappedAction(action));
      }

      @Override
      public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
        return delegateWorker.schedule(new ExecutionWrappedAction(action), delayTime, unit);
      }

      @Override
      public void unsubscribe() {
        delegateWorker.unsubscribe();
      }

      @Override
      public boolean isUnsubscribed() {
        return delegateWorker.isUnsubscribed();
      }

      @Override
      public long now() {
        return delegateWorker.now();
      }
    };
  }
}
