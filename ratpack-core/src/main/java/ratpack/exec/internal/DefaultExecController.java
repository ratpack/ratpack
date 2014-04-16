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

import com.google.common.util.concurrent.*;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.handling.internal.InterceptedOperation;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class DefaultExecController implements ExecController {

  private static final ThreadLocal<ExecController> THREAD_BINDING = new ThreadLocal<>();

  private final ThreadLocal<ExecContext.Supplier> contextSupplierThreadLocal = new ThreadLocal<>();
  private final ThreadLocal<List<Runnable>> onExecFinish = new ThreadLocal<List<Runnable>>() {
    @Override
    protected List<Runnable> initialValue() {
      return new LinkedList<>();
    }
  };

  private final ListeningScheduledExecutorService computeExecutor;
  private final ListeningExecutorService blockingExecutor;
  private final EventLoopGroup eventLoopGroup;

  public DefaultExecController(int numThreads) {
    this.eventLoopGroup = new NioEventLoopGroup(numThreads, new ExecControllerBindingThreadFactory("ratpack-compute", Thread.MAX_PRIORITY));
    this.computeExecutor = MoreExecutors.listeningDecorator(eventLoopGroup);
    this.blockingExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ExecControllerBindingThreadFactory("ratpack-blocking", Thread.NORM_PRIORITY)));
  }

  public static ExecController getThreadBoundController() {
    return THREAD_BINDING.get();
  }

  public static ExecContext getThreadBoundContext() {
    return getThreadBoundController().getContext();
  }

  public void shutdown() throws Exception {
    eventLoopGroup.shutdownGracefully();
    blockingExecutor.shutdown();
  }

  @Override
  public ExecContext getContext() throws NoBoundContextException {
    ExecContext.Supplier contextSupplier = contextSupplierThreadLocal.get();
    if (contextSupplier == null) {
      throw new NoBoundContextException("No context is bound to the current thread (are you calling this from a blocking operation?)");
    } else {
      return contextSupplier.get();
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

  public void exec(ExecContext.Supplier execContextSupplier, Action<? super ExecContext> action) {
    exec(execContextSupplier, ExecInterceptor.ExecType.COMPUTE, action);
  }

  private void exec(ExecContext.Supplier execContextSupplier, final ExecInterceptor.ExecType execType, final Action<? super ExecContext> action) {

    try {
      contextSupplierThreadLocal.set(execContextSupplier);
      new InterceptedOperation(execType, getContext().getInterceptors()) {
        @Override
        protected void performOperation() throws Exception {
          action.execute(getContext());
        }
      }.run();
    } catch (Throwable e) {
      onExecFinish.get().clear();
      ExecException.wrapAndForward(getContext(), e);
    } finally {
      contextSupplierThreadLocal.remove();
    }

    List<Runnable> runnables = onExecFinish.get();
    while (!runnables.isEmpty()) {
      runnables.remove(0).run();
    }
  }

  @Override
  public <T> Promise<T> blocking(final Callable<T> operation) {
    final ExecContext context = getContext();
    return context.promise(new Action<Fulfiller<? super T>>() {
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
          exec(context.getSupplier(), ExecInterceptor.ExecType.BLOCKING, new Action<ExecContext>() {
            @Override
            public void execute(ExecContext thing) throws Exception {
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
  public <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return new DefaultSuccessOrErrorPromise<>(getContext(), this, action);
  }

  @Override
  public void onExecFinish(Runnable runnable) {
    onExecFinish.get().add(runnable);
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
}
