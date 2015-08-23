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
import io.netty.util.concurrent.EventExecutor;
import ratpack.exec.ExecInitializer;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.ExecStarter;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.registry.RegistrySpec;
import ratpack.util.internal.ChannelImplDetector;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ratpack.func.Action.noop;

public class DefaultExecController implements ExecControllerInternal {

  private static final Action<Throwable> LOG_UNCAUGHT = t -> DefaultExecution.LOGGER.error("Uncaught execution exception", t);
  private static final int MAX_ERRORS_THRESHOLD = 5;

  private final ExecutorService blockingExecutor;
  private final EventLoopGroup eventLoopGroup;
  private final int numThreads;
  private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

  private ImmutableList<? extends ExecInterceptor> interceptors = ImmutableList.of();
  private ImmutableList<? extends ExecInitializer> initializers = ImmutableList.of();

  public DefaultExecController() {
    this(Runtime.getRuntime().availableProcessors() * 2);
  }

  public DefaultExecController(int numThreads) {
    this.numThreads = numThreads;
    this.eventLoopGroup = ChannelImplDetector.eventLoopGroup(numThreads, new ExecControllerBindingThreadFactory(true, "ratpack-compute", Thread.MAX_PRIORITY));
    this.blockingExecutor = Executors.newCachedThreadPool(new ExecControllerBindingThreadFactory(false, "ratpack-blocking", Thread.NORM_PRIORITY));
  }

  @Override
  public void setInterceptors(ImmutableList<? extends ExecInterceptor> interceptors) {
    this.interceptors = interceptors;
  }

  @Override
  public void setInitializers(ImmutableList<? extends ExecInitializer> initializers) {
    this.initializers = initializers;
  }

  @Override
  public ImmutableList<? extends ExecInterceptor> getInterceptors() {
    return interceptors;
  }

  @Override
  public ImmutableList<? extends ExecInitializer> getInitializers() {
    return initializers;
  }

  public void close() {
    eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    blockingExecutor.shutdown();
    try {
      boolean inEventLoop = false;
      for (EventExecutor eventExecutor : eventLoopGroup.children()) {
        if (eventExecutor.inEventLoop()) {
          inEventLoop = true;
          break;
        }
      }
      if (!inEventLoop) {
        eventLoopGroup.awaitTermination(10, TimeUnit.MINUTES);
      }
      blockingExecutor.awaitTermination(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
        Thread.currentThread().setContextClassLoader(contextClassLoader);
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
  public ExecStarter fork() {
    return new ExecStarter() {
      private Action<? super Throwable> onError = LOG_UNCAUGHT;
      private Action<? super Execution> onComplete = noop();
      private Action<? super Execution> onStart = noop();
      private Action<? super RegistrySpec> registry = noop();
      private EventLoop eventLoop = getEventLoopGroup().next();

      @Override
      public ExecStarter eventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
        return this;
      }

      @Override
      public ExecStarter onError(Action<? super Throwable> onError) {
        List<Throwable> seen = Lists.newArrayListWithCapacity(0);
        this.onError = t -> {
          if (seen.size() < MAX_ERRORS_THRESHOLD) {
            seen.add(t);
            onError.execute(t);
          } else {
            seen.forEach(t::addSuppressed);
            DefaultExecution.LOGGER.error("Error handler " + onError + "reached maximum error threshold (might be caught in an error loop)", t);
          }
        };
        return this;
      }

      @Override
      public ExecStarter onComplete(Action<? super Execution> onComplete) {
        this.onComplete = onComplete;
        return this;
      }

      @Override
      public ExecStarter onStart(Action<? super Execution> onStart) {
        this.onStart = onStart;
        return this;
      }

      @Override
      public ExecStarter register(Action<? super RegistrySpec> action) {
        this.registry = action;
        return this;
      }

      @Override
      public void start(Action<? super Execution> initialExecutionSegment) {
        if (eventLoop.inEventLoop() && DefaultExecution.get() == null) {
          try {
            new DefaultExecution(DefaultExecController.this, eventLoop, registry, initialExecutionSegment, onError, onStart, onComplete);
          } catch (Exception e) {
            throw new InternalError("could not start execution", e);
          }
        } else {
          eventLoop.submit(() ->
              new DefaultExecution(DefaultExecController.this, eventLoop, registry, initialExecutionSegment, onError, onStart, onComplete)
          );
        }
      }
    };
  }

}
