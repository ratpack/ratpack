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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import ratpack.exec.ExecBuilder;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.registry.RegistrySpec;
import ratpack.util.Exceptions;
import ratpack.util.internal.ChannelImplDetector;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ratpack.func.Action.noop;

public class DefaultExecController implements ExecControllerInternal {

  private static final BiAction<Execution, Throwable> LOG_UNCAUGHT = (o, t) -> ExecutionBacking.LOGGER.error("Uncaught execution exception", t);
  private static final int MAX_ERRORS_THRESHOLD = 5;

  private final ExecutorService blockingExecutor;
  private final EventLoopGroup eventLoopGroup;
  private final int numThreads;

  private ImmutableList<? extends ExecInterceptor> interceptors = ImmutableList.of();

  public DefaultExecController() {
    this(Runtime.getRuntime().availableProcessors() * 2);
  }

  public DefaultExecController(int numThreads) {
    this.numThreads = numThreads;
    this.eventLoopGroup = ChannelImplDetector.eventLoopGroup(numThreads, new ExecControllerBindingThreadFactory(true, "ratpack-compute", Thread.MAX_PRIORITY));
    this.blockingExecutor = Executors.newCachedThreadPool(new ExecControllerBindingThreadFactory(false, "ratpack-blocking", Thread.NORM_PRIORITY));
  }

  @Override
  public void setDefaultInterceptors(ImmutableList<? extends ExecInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  public void close() {
    eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    blockingExecutor.shutdown();
  }

  @Override
  public ScheduledExecutorService getExecutor() {
    return eventLoopGroup;
  }

  @Override
  public ExecutorService getBlockingExecutor() {
    return blockingExecutor;
  }

  @Override
  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  private class ExecControllerBindingThreadFactory extends DefaultThreadFactory {
    private final boolean compute;

    public ExecControllerBindingThreadFactory(boolean compute, String name, int priority) {
      super(name, priority);
      this.compute = compute;
    }

    @Override
    public Thread newThread(final Runnable r) {
      return super.newThread(() -> {
        ThreadBinding.bind(compute, DefaultExecController.this);
        r.run();
      });
    }
  }

  @Override
  public boolean isManagedThread() {
    return ThreadBinding.get().map(c -> c.getExecController() == this).orElse(false);
  }

  @Override
  public int getNumThreads() {
    return numThreads;
  }

  @Override
  public ExecBuilder exec() {
    return new ExecBuilder() {
      private BiAction<? super Execution, ? super Throwable> onError = LOG_UNCAUGHT;
      private Action<? super Execution> onComplete = noop();
      private Action<? super Execution> onStart = noop();
      private Action<? super RegistrySpec> registry = noop();
      private EventLoop eventLoop = getEventLoopGroup().next();

      @Override
      public ExecBuilder eventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
        return this;
      }

      @Override
      public ExecBuilder onError(BiAction<? super Execution, ? super Throwable> onError) {
        List<Throwable> seen = Lists.newLinkedList();
        this.onError = (e, t) -> {
          if (seen.size() < MAX_ERRORS_THRESHOLD) {
            seen.add(t);
            onError.execute(e, t);
          } else {
            seen.forEach(t::addSuppressed);
            ExecutionBacking.LOGGER.error("Error handler " + onError + "reached maximum error threshold (might be caught in an error loop)", t);
          }
        };
        return this;
      }

      @Override
      public ExecBuilder onError(Action<? super Throwable> onError) {
        return onError((e, t) -> onError.execute(t));
      }

      @Override
      public ExecBuilder onComplete(Action<? super Execution> onComplete) {
        this.onComplete = onComplete;
        return this;
      }

      @Override
      public ExecBuilder onStart(Action<? super Execution> onStart) {
        this.onStart = onStart;
        return this;
      }

      @Override
      public ExecBuilder register(Action<? super RegistrySpec> action) {
        this.registry = action;
        return this;
      }

      @Override
      public void start(Action<? super Execution> action) {
        if (eventLoop.inEventLoop() && ExecutionBacking.get() == null) {
          Exceptions.uncheck(() -> new ExecutionBacking(DefaultExecController.this, eventLoop, interceptors, registry, action, onError, onStart, onComplete));
        } else {
          eventLoop.submit(() ->
              new ExecutionBacking(DefaultExecController.this, eventLoop, interceptors, registry, action, onError, onStart, onComplete)
          );
        }
      }
    };
  }

}
