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
import org.reactivestreams.spi.Publisher;
import org.reactivestreams.spi.Subscriber;
import org.reactivestreams.spi.Subscription;
import ratpack.exec.*;
import ratpack.exec.ExecutionException;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.handling.internal.InterceptedOperation;
import ratpack.registry.internal.SimpleMutableRegistry;
import ratpack.util.ExceptionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultExecController implements ExecController {

  private static final ThreadLocal<ExecController> THREAD_BINDING = new ThreadLocal<>();

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultExecController.class);

  private final ThreadLocal<Execution> executionHolder = new ThreadLocal<>();

  private final ListeningScheduledExecutorService computeExecutor;
  private final ListeningExecutorService blockingExecutor;
  private final EventLoopGroup eventLoopGroup;
  private final ExecControl control;
  private final int numThreads;

  public DefaultExecController(int numThreads) {
    this.numThreads = numThreads;
    this.eventLoopGroup = new NioEventLoopGroup(numThreads, new ExecControllerBindingThreadFactory("ratpack-compute", Thread.MAX_PRIORITY));
    this.computeExecutor = MoreExecutors.listeningDecorator(eventLoopGroup);
    this.blockingExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ExecControllerBindingThreadFactory("ratpack-blocking", Thread.NORM_PRIORITY)));
    this.control = new FactoryBackedExecControl();
  }

  public static Optional<ExecController> getThreadBoundController() {
    return Optional.fromNullable(THREAD_BINDING.get());
  }

  public void close() {
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
  public void start(final Action<? super ratpack.exec.Execution> action) {
    if (!isManagedThread() || executionHolder.get() != null) {
      computeExecutor.submit(new Runnable() {
        @Override
        public void run() {
          start(action);
        }
      });
    } else {
      new Execution(action);
    }
  }

  private class FactoryBackedExecControl implements ExecControl {

    private final Factory<Execution> executionFactory = new Factory<Execution>() {
      @Override
      public Execution create() {
        return getExecution();
      }
    };

    @Override
    public <T> Promise<T> blocking(final Callable<T> blockingOperation) {
      return promise(new Action<Fulfiller<? super T>>() {
        @Override
        public void execute(final Fulfiller<? super T> fulfiller) throws Exception {
          final Execution execution = getExecution();
          ListenableFuture<T> future = blockingExecutor.submit(new BlockingOperation(execution));
          Futures.addCallback(future, new ComputeResume(fulfiller), computeExecutor);
        }

        class BlockingOperation implements Callable<T> {
          private final Execution execution;
          private Exception exception;
          private T result;

          public BlockingOperation(Execution execution) {
            this.execution = execution;
          }

          @Override
          public T call() throws Exception {
            execution.intercept(ExecInterceptor.ExecType.BLOCKING, execution.interceptors, new Action<Execution>() {
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
      return new DefaultPromise<>(executionFactory, action);
    }

    @Override
    public void fork(Action<? super ratpack.exec.Execution> action) {
      start(action);
    }

    @Override
    public <T> void stream(Publisher<T> publisher, final Subscriber<T> subscriber) {
      publisher.subscribe(new Subscriber<T>() {
        final Execution execution = getExecution();

        @Override
        public void onSubscribe(final Subscription subscription) {
          this.execution.continueVia(new Runnable() {
            @Override
            public void run() {
              subscriber.onSubscribe(subscription);
            }
          });
        }

        @Override
        public void onNext(final T element) {
          this.execution.continueExecutionAndWait(new Action<ratpack.exec.Execution>() {
            @Override
            public void execute(ratpack.exec.Execution execution) throws Exception {
              subscriber.onNext(element);
            }
          });
        }

        @Override
        public void onComplete() {
          this.execution.joinExecutionEnd(new Action<ratpack.exec.Execution>() {
            @Override
            public void execute(ratpack.exec.Execution execution) throws Exception {
              subscriber.onComplete();
            }
          });
        }

        @Override
        public void onError(final Throwable cause) {
          this.execution.joinExecutionEnd(new Action<ratpack.exec.Execution>() {
            @Override
            public void execute(ratpack.exec.Execution execution) throws Exception {
              subscriber.onError(cause);
            }
          });
        }
      });
    }
  }

  public class Execution extends SimpleMutableRegistry implements ratpack.exec.Execution {
    private final List<ExecInterceptor> interceptors = new LinkedList<>();
    private final Deque<Runnable> segments = new ConcurrentLinkedDeque<>();
    private final Queue<Runnable> onCompletes = new ConcurrentLinkedQueue<>();

    private Action<? super Throwable> errorHandler = new Action<Throwable>() {
      @Override
      public void execute(Throwable throwable) throws Exception {
        throw ExceptionUtils.toException(throwable);
      }
    };

    private final AtomicBoolean active = new AtomicBoolean();
    private boolean waiting;
    private boolean done;

    public Execution(Action<? super ratpack.exec.Execution> action) {
      segments.addLast(new UserCodeSegment(action));
      tryDrain();
    }

    @Override
    public <T> Promise<T> blocking(final Callable<T> operation) {
      return control.blocking(operation);
    }


    @Override
    public <T> Promise<T> promise(final Action<? super Fulfiller<T>> action) {
      return control.promise(action);
    }

    @Override
    public void fork(Action<? super ratpack.exec.Execution> action) {
      control.fork(action);
    }

    @Override
    public <T> void stream(Publisher<T> publisher, Subscriber<T> subscriber) {
      control.stream(publisher, subscriber);
    }

    @Override
    public void setErrorHandler(Action<? super Throwable> errorHandler) {
      this.errorHandler = errorHandler;
    }

    @Override
    public void addInterceptor(ExecInterceptor execInterceptor, Action<? super ratpack.exec.Execution> continuation) throws Exception {
      interceptors.add(execInterceptor);
      intercept(ExecInterceptor.ExecType.COMPUTE, Collections.singletonList(execInterceptor), continuation);
    }

    @Override
    public ExecController getController() {
      return DefaultExecController.this;
    }


    public void join(final Action<? super ratpack.exec.Execution> action) {
      segments.addFirst(new UserCodeSegment(action));
      waiting = false;
      tryDrain();
    }

    public void joinExecutionEnd(final Action<? super ratpack.exec.Execution> action) {
      segments.addLast(new UserCodeSegment(action));
      waiting = false;
      tryDrain();
    }

    public void continueExecutionAndWait(final Action<? super ratpack.exec.Execution> action) {
      segments.add(new UserCodeSegment(action));
      waiting = true;
      tryDrain();
    }

    public void joinExecutionEnd(final Action<? super ratpack.exec.Execution> action) {
      segments.addLast(new UserCodeSegment(action));
      waiting = false;
      tryDrain();
    }

    public void continueExecutionAndWait(final Action<? super ratpack.exec.Execution> action) {
      segments.add(new UserCodeSegment(action));
      waiting = true;
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
      assertNotDone();
      if (!segments.isEmpty()) {
        if (active.compareAndSet(false, true)) {
          drain();
        }
      }
    }

    private void runOnCompletes() {
      Runnable onComplete = onCompletes.poll();
      while (onComplete != null) {
        try {
          onComplete.run();
        } catch (Exception e) {
          LOGGER.warn("", e);
        } finally {
          onComplete = onCompletes.poll();
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
            if (waiting) { // the segment initiated an async op
              break;
            } else {
              segment = segments.poll();
              if (segment == null) { // not waiting and no more segments, we are done
                done = true;
                runOnCompletes();
              }
            }
          }
        } finally {
          executionHolder.remove();
          active.set(false);
        }
        if (waiting) {
          tryDrain();
        }
      } else {
        active.set(false);
        eventLoopGroup.submit(new Runnable() {
          @Override
          public void run() {
            tryDrain();
          }
        });
      }
    }

    @Override
    public void onComplete(Runnable runnable) {
      assertNotDone();
      onCompletes.add(runnable);
    }

    private void assertNotDone() {
      if (done) {
        throw new ExecutionException("execution is complete");
      }
    }

    public void intercept(final ExecInterceptor.ExecType execType, final List<ExecInterceptor> interceptors, final Action<? super Execution> action) throws Exception {
      new InterceptedOperation(execType, interceptors) {
        @Override
        protected void performOperation() throws Exception {
          action.execute(Execution.this);
        }
      }.run();
    }

    /**
     * An execution segment that executes “user code” (i.e. not execution management infrastructure)
     */
    private class UserCodeSegment implements Runnable {
      private final Action<? super ratpack.exec.Execution> action;

      public UserCodeSegment(Action<? super ratpack.exec.Execution> action) {
        this.action = action;
      }

      @Override
      public void run() {
        try {
          try {
            intercept(ExecInterceptor.ExecType.COMPUTE, interceptors, action);
          } catch (ExecutionSegmentTerminationError e) {
            throw e.getCause();
          }
        } catch (final Throwable e) {
          segments.clear();
          segments.addFirst(new Runnable() {
            @Override
            public void run() {
              try {
                errorHandler.execute(e);
              } catch (final Throwable e) {
                segments.addFirst(new UserCodeSegment(new Action<ratpack.exec.Execution>() {
                  @Override
                  public void execute(ratpack.exec.Execution execution) throws Exception {
                    throw e;
                  }
                }));
              }
            }
          });
        }
      }
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

  @Override
  public int getNumThreads() {
    return numThreads;
  }

}
