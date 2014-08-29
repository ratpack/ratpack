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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.Actions;
import ratpack.func.Factory;

import java.util.Collections;
import java.util.concurrent.Callable;

public class DefaultExecControl implements ExecControl {

  private final ExecController execController;
  private final ThreadLocal<ExecutionBacking> threadBinding = new ThreadLocal<>();
  private final Factory<ExecutionBacking> executionBackingFactory = new Factory<ExecutionBacking>() {
    @Override
    public ExecutionBacking create() {
      return getBacking();
    }
  };

  public DefaultExecControl(ExecController execController) {
    this.execController = execController;
  }

  private ExecutionBacking getBacking() {
    ExecutionBacking executionBacking = threadBinding.get();
    if (executionBacking == null) {
      throw new ExecutionException("Current thread has no bound execution");
    } else {
      return executionBacking;
    }
  }

  @Override
  public Execution getExecution() {
    return getBacking().getExecution();
  }

  @Override
  public ExecController getController() {
    return execController;
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, Action<? super Execution> continuation) throws Exception {
    ExecutionBacking backing = getBacking();
    backing.getInterceptors().add(execInterceptor);
    backing.intercept(ExecInterceptor.ExecType.COMPUTE, Collections.singletonList(execInterceptor), continuation);
  }

  @Override
  public <T> Promise<T> blocking(final Callable<T> blockingOperation) {
    final ExecutionBacking backing = getBacking();
    final ExecController controller = backing.getController();
    return promise(new Action<Fulfiller<? super T>>() {
      @Override
      public void execute(final Fulfiller<? super T> fulfiller) throws Exception {
        ListenableFuture<T> future = controller.getBlockingExecutor().submit(new BlockingOperation());
        Futures.addCallback(future, new ComputeResume(fulfiller), controller.getExecutor());
      }

      class BlockingOperation implements Callable<T> {
        private Exception exception;
        private T result;

        @Override
        public T call() throws Exception {
          backing.intercept(ExecInterceptor.ExecType.BLOCKING, backing.getInterceptors(), new Action<Execution>() {
            @Override
            public void execute(Execution execution) throws Exception {
              try {
                result = blockingOperation.call();
              } catch (Exception e) {
                exception = e;
              }
            }
          });

          if (exception != null) {
            throw exception;
          } else {
            return result;
          }
        }
      }

      class ComputeResume implements FutureCallback<T> {
        private final Fulfiller<? super T> fulfiller;

        public ComputeResume(Fulfiller<? super T> fulfiller) {
          this.fulfiller = fulfiller;
        }

        @Override
        public void onSuccess(final T result) {
          fulfiller.success(result);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void onFailure(final Throwable t) {
          fulfiller.error(t);
        }
      }
    });

  }


  @Override
  public <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return new DefaultPromise<>(executionBackingFactory, action);
  }

  @Override
  public void fork(Action<? super Execution> action) {
    fork(action, Actions.throwException(), Actions.noop());
  }

  @Override
  public void fork(Action<? super Execution> action, Action<? super Throwable> onError) {
    fork(action, onError, Actions.noop());
  }

  @Override
  public void fork(final Action<? super Execution> action, final Action<? super Throwable> onError, final Action<? super Execution> onComplete) {
    if (execController.isManagedThread() && threadBinding.get() == null) {
      new ExecutionBacking(execController, threadBinding, action, onError, onComplete);
    } else {
      execController.getExecutor().submit(new Runnable() {
        @Override
        public void run() {
          new ExecutionBacking(execController, threadBinding, action, onError, onComplete);
        }
      });
    }
  }

  @Override
  public <T> void stream(Publisher<T> publisher, final Subscriber<? super T> subscriber) {
    final ExecutionBacking executionBacking = getBacking();

    publisher.subscribe(new Subscriber<T>() {
      @Override
      public void onSubscribe(final Subscription subscription) {
        executionBacking.streamExecution(new Action<Execution>() {
          @Override
          public void execute(Execution execution) throws Exception {
            subscriber.onSubscribe(subscription);
          }
        });
      }

      @Override
      public void onNext(final T element) {
        executionBacking.streamExecution(new Action<Execution>() {
          @Override
          public void execute(Execution execution) throws Exception {
            subscriber.onNext(element);
          }
        });
      }

      @Override
      public void onComplete() {
        executionBacking.completeStreamExecution(new Action<Execution>() {
          @Override
          public void execute(Execution execution) throws Exception {
            subscriber.onComplete();
          }
        });
      }

      @Override
      public void onError(final Throwable cause) {
        executionBacking.completeStreamExecution(new Action<Execution>() {
          @Override
          public void execute(Execution execution) throws Exception {
            subscriber.onError(cause);
          }
        });
      }
    });
  }

}
