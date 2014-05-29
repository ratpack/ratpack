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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.*;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.handling.internal.InterceptedOperation;
import ratpack.registry.internal.SimpleMutableRegistry;
import ratpack.util.ExceptionUtils;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultExecController implements ExecController {

  private static final ThreadLocal<ExecController> THREAD_BINDING = new ThreadLocal<>();

  private final ThreadLocal<Execution> executionHolder = new ThreadLocal<>();

  private final ListeningScheduledExecutorService computeExecutor;
  private final ListeningExecutorService blockingExecutor;
  private final EventLoopGroup eventLoopGroup;
  private final ExecControl control;

  public DefaultExecController(int numThreads) {
    this.eventLoopGroup = new NioEventLoopGroup(numThreads, new ExecControllerBindingThreadFactory("ratpack-compute", Thread.MAX_PRIORITY));
    this.computeExecutor = MoreExecutors.listeningDecorator(eventLoopGroup);
    this.blockingExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ExecControllerBindingThreadFactory("ratpack-blocking", Thread.NORM_PRIORITY)));
    this.control = new Control();
  }

  public static Optional<ExecController> getThreadBoundController() {
    return Optional.fromNullable(THREAD_BINDING.get());
  }

  public void shutdown() throws Exception {
    eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    blockingExecutor.shutdown();
  }

  @Override
  public Execution getExecution() throws ExecutionException {
    Execution execution = executionHolder.get();
    if (execution == null) {
      throw new ExecutionException("No execution is bound to the current thread (are you calling this from a blocking operation or a manual created thread?)");
    } else {
      return execution;
    }
  }

  @Override
  public ListeningScheduledExecutorService getExecutor() {
    return computeExecutor;
  }

  @Override
  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  @Override
  public void start(Action<? super ratpack.exec.Execution> action) {
    new Execution().resume(action);
  }

  private static class ErrorContext {
    private final Throwable error;
    private final Action<? super Throwable> handler;

    private ErrorContext(Throwable error, Action<? super Throwable> handler) {
      this.error = error;
      this.handler = handler;
    }

    public void apply() throws Exception {
      handler.execute(error);
    }

  }

  public class Execution extends SimpleMutableRegistry implements ratpack.exec.Execution {
    private final List<ExecInterceptor> interceptors = new LinkedList<>();
    private final Deque<Runnable> segments = new ConcurrentLinkedDeque<>();

    private Action<? super Throwable> errorHandler = new Action<Throwable>() {
      @Override
      public void execute(Throwable throwable) throws Exception {
        throw ExceptionUtils.toException(throwable);
      }
    };
    private ErrorContext errorContext;

    private final AtomicBoolean active = new AtomicBoolean();
    private boolean waiting;
    private boolean done;

    @Override
    public void setErrorHandler(Action<? super Throwable> errorHandler) {
      this.errorHandler = errorHandler;
    }

    @Override
    public void addInterceptor(ExecInterceptor execInterceptor, Action<? super ratpack.exec.Execution> continuation) throws Exception {
      interceptors.add(execInterceptor);
      intercept(ExecInterceptor.ExecType.COMPUTE, Collections.singletonList(execInterceptor), continuation);
    }

    private void assertBound() {
      if (getExecution() != this) {
        throw new ExecutionException("execution overlap");
      }
    }

    @Override
    public void error(Throwable throwable) {
      assertBound();
      errorContext = new ErrorContext(throwable, errorHandler);
    }

    @Override
    public ExecController getController() {
      return DefaultExecController.this;
    }

    public void resume(final Action<? super ratpack.exec.Execution> action) {
      segments.addLast(new ComputeRunnable(action));
      tryDrain();
    }

    public void join(final Action<? super ratpack.exec.Execution> action) {
      segments.addFirst(new ComputeRunnable(action));
      waiting = false;
      tryDrain();
    }

    public void continueVia(final Runnable runnable) {
      segments.addFirst(new Runnable() {
        @Override
        public void run() {
          waiting = true;
          runnable.run();
        }
      });
    }

    private void tryDrain() {
      if (!done && !waiting && !segments.isEmpty()) {
        if (active.compareAndSet(false, true)) {
          drain();
        }
      }
    }

    private void drain() {
      if (isManagedThread()) {
        executionHolder.set(this);
        try {
          Runnable segment = segments.poll();
          while (segment != null) {
            segment.run();
            if (done) {
              return;
            } else {
              segment = waiting ? null : segments.poll();
            }
          }
        } finally {
          executionHolder.remove();
          active.set(false);
        }

        tryDrain();
      } else {
        eventLoopGroup.submit(new Runnable() {
          @Override
          public void run() {
            drain();
          }
        });
      }
    }

    @Override
    public void complete() {
      done = true;
    }

    public void intercept(final ExecInterceptor.ExecType execType, final List<ExecInterceptor> interceptors, final Action<? super Execution> action) throws Exception {
      new InterceptedOperation(execType, interceptors) {
        @Override
        protected void performOperation() throws Exception {
          action.execute(Execution.this);
        }
      }.run();
    }

    private class ComputeRunnable implements Runnable {
      private final Action<? super ratpack.exec.Execution> action;

      public ComputeRunnable(Action<? super ratpack.exec.Execution> action) {
        this.action = action;
      }

      @Override
      public void run() {
        try {
          intercept(ExecInterceptor.ExecType.COMPUTE, interceptors, action);
        } catch (Throwable e) {
          error(e);
        }

        if (errorContext != null) {
          try {
            segments.addLast(new Runnable() {
              @Override
              public void run() {
                done = true;
              }
            });
            errorContext.apply();
          } catch (Throwable e) {
            // TODO - error was unhandled, have to deal with it somehow
            e.printStackTrace();
          }
        }
      }
    }
  }

  private class Control implements ExecControl {
    @Override
    public <T> Promise<T> blocking(final Callable<T> operation) {
      final Execution execution = getExecution();
      return promise(new Action<Fulfiller<? super T>>() {
        @Override
        public void execute(final Fulfiller<? super T> fulfiller) throws Exception {
          ListenableFuture<T> future = blockingExecutor.submit(new BlockingOperation());
          Futures.addCallback(future, new ComputeResume(fulfiller), computeExecutor);
        }

        class BlockingOperation implements Callable<T> {
          private Exception exception;
          private T result;

          @Override
          public T call() throws Exception {
            execution.intercept(ExecInterceptor.ExecType.BLOCKING, execution.interceptors, new Action<Execution>() {
              @Override
              public void execute(Execution execution) throws Exception {
                try {
                  result = operation.call();
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
    public <T> Promise<T> promise(final Action<? super Fulfiller<T>> action) {
      return new DefaultPromise<>(getExecution(), action);
    }

  }

  @Override
  public ExecControl getControl() {
    return control;
  }

  private class ExecControllerBindingThreadFactory extends DefaultThreadFactory {
    public ExecControllerBindingThreadFactory(String name, int priority) {
      super(name, priority);
    }

    @Override
    public Thread newThread(final Runnable r) {
      return super.newThread(new Runnable() {
        @Override
        public void run() {
          THREAD_BINDING.set(DefaultExecController.this);
          r.run();
        }
      });
    }

  }

  @Override
  public boolean isManagedThread() {
    Optional<ExecController> threadBoundController = getThreadBoundController();
    return threadBoundController.isPresent() && threadBoundController.get() == this;
  }

}
