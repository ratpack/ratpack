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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.*;
import ratpack.func.*;
import ratpack.exec.registry.RegistrySpec;
import ratpack.exec.util.internal.InternalRatpackError;
import ratpack.exec.util.internal.TransportDetector;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.func.Action.noop;

public class DefaultExecController implements ExecControllerInternal {

  private static final Action<Throwable> LOG_UNCAUGHT = t -> DefaultExecution.LOGGER.error("Uncaught execution exception", t);

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecController.class);

  private final ExecutorService blockingExecutor;
  private final EventLoopGroup eventLoopGroup;
  private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
  private final AtomicBoolean closed = new AtomicBoolean();

  private ImmutableList<? extends ExecInterceptor> interceptors;
  private ImmutableList<? extends ExecInitializer> initializers;

  private Queue<Block> onClose = new ConcurrentLinkedQueue<>();

  public DefaultExecController(Spec spec) {

    eventLoopGroup = Exceptions.uncheck(() -> of(new Compute(), spec.computeSpec));
    blockingExecutor = Exceptions.uncheck(() -> of(new Blocking(), spec.blockingSpec));

    this.initializers = ImmutableList.copyOf(spec.initializers);
    this.interceptors = ImmutableList.copyOf(spec.interceptors);
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

  @Override
  public boolean onClose(Block block) {
    if (closed.get()) {
      return false;
    } else {
      onClose.add(block);
      return true;
    }
  }

  public void close() {
    Block onClose = this.onClose.poll();
    while (onClose != null) {
      try {
        onClose.execute();
      } catch (Exception e) {
        LOGGER.warn("Exception thrown by exec controller onClose callback will be ignored - ", e);
      }
      onClose = this.onClose.poll();
    }
    blockingExecutor.shutdown();
    eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
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

  public static ExecController of(Action<? super ExecControllerSpec> definition) throws Exception {
    Spec spec = new Spec();
    definition.execute(spec);

    return new DefaultExecController(spec);
  }

  protected EventLoopGroup of(Compute binding, Action<? super ExecControllerSpec.ComputeSpec> definition) throws Exception {
    definition.execute(binding);
    return binding.eventLoopGroupFactory.apply(binding.threads, new ExecControllerBindingThreadFactory(true, binding.prefix, binding.priority));
  }

  protected ExecutorService of(Blocking binding, Action<? super ExecControllerSpec.BlockingSpec> definition) throws Exception {
    definition.execute(binding);
    return binding.executorFactory.apply(new ExecControllerBindingThreadFactory(false, binding.prefix, binding.priority));
  }

  private class ExecControllerBindingThreadFactory extends DefaultThreadFactory {
    private final boolean compute;

    ExecControllerBindingThreadFactory(boolean compute, String name, int priority) {
      super(name, priority);
      this.compute = compute;
    }

    @Override
    public Thread newThread(final Runnable r) {
      return super.newThread(() -> {
        ExecThreadBinding.bind(compute, DefaultExecController.this);
        Thread.currentThread().setContextClassLoader(contextClassLoader);
        r.run();
      });
    }
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
        this.onError = onError;
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
        DefaultExecution current = DefaultExecution.get();
        DefaultExecution execution = createExecution(initialExecutionSegment, current == null ? null : current.getRef());
        if (eventLoop.inEventLoop() && current == null) {
          execution.drain();
        } else {
          eventLoop.submit(execution::drain);
        }
      }

      private DefaultExecution createExecution(Action<? super Execution> initialExecutionSegment, ExecutionRef parentRef) {
        try {
          return new DefaultExecution(
            DefaultExecController.this,
            parentRef,
            eventLoop,
            registry,
            initialExecutionSegment,
            onError,
            onStart,
            onComplete
          );
        } catch (Throwable e) {
          throw new InternalRatpackError("could not start execution", e);
        }
      }
    };
  }

  public static class EventLoopGroupBinding implements ExecControllerSpec.EventLoopSpec {

    protected int threads;
    protected String prefix;
    protected int priority;
    protected BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory = TransportDetector::eventLoopGroup;

    @Override
    public ExecControllerSpec.EventLoopSpec threads(int threads) {
      this.threads = threads;
      return this;
    }

    @Override
    public ExecControllerSpec.EventLoopSpec prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    @Override
    public ExecControllerSpec.EventLoopSpec priority(int priority) {
      this.priority = priority;
      return this;
    }

    @Override
    public ExecControllerSpec.EventLoopSpec eventLoopGroup(BiFunction<Integer, ThreadFactory, io.netty.channel.EventLoopGroup> eventLoopGroupFactory) {
      this.eventLoopGroupFactory = eventLoopGroupFactory;
      return this;
    }
  }

  public static class Compute extends EventLoopGroupBinding implements ExecControllerSpec.ComputeSpec {

    public Compute() {
      this.threads = Runtime.getRuntime().availableProcessors() * 2;
      this.prefix = "ratpack-compute";
      this.priority = Thread.MAX_PRIORITY;
    }

    @Override
    public ExecControllerSpec.ComputeSpec threads(int threads) {
      this.threads = threads;
      return this;
    }

    @Override
    public ExecControllerSpec.ComputeSpec prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    @Override
    public ExecControllerSpec.ComputeSpec priority(int priority) {
      this.priority = priority;
      return this;
    }

    @Override
    public ExecControllerSpec.ComputeSpec eventLoopGroup(BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory) {
      this.eventLoopGroupFactory = eventLoopGroupFactory;
      return this;
    }
  }

  public static class Spec implements ExecControllerSpec {

    private Action<? super ComputeSpec> computeSpec = Action.noop();
    private Action<? super BlockingSpec> blockingSpec = Action.noop();
    private List<ExecInterceptor> interceptors = Lists.newArrayList();
    private List<ExecInitializer> initializers = Lists.newArrayList();

    @Override
    public ExecControllerSpec interceptor(ExecInterceptor interceptor) {
      this.interceptors.add(interceptor);
      return this;
    }

    @Override
    public ExecControllerSpec initializer(ExecInitializer initializer) {
      this.initializers.add(initializer);
      return this;
    }

    @Override
    public ExecControllerSpec compute(Action<? super ComputeSpec> definition) {
      this.computeSpec = definition;
      return this;
    }

    @Override
    public ExecControllerSpec blocking(Action<? super BlockingSpec> definition) {
      this.blockingSpec = definition;
      return this;
    }
  }

  public static class ExecutorServiceBinding implements ExecControllerSpec.ExecutorServiceSpec {
    protected String prefix;
    protected int priority;
    protected Function<ThreadFactory, ExecutorService> executorFactory = Executors::newCachedThreadPool;

    @Override
    public ExecControllerSpec.ExecutorServiceSpec prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    @Override
    public ExecControllerSpec.ExecutorServiceSpec priority(int priority) {
      this.priority = priority;
      return this;
    }

    @Override
    public ExecControllerSpec.ExecutorServiceSpec executor(Function<ThreadFactory, ExecutorService> executorFactory) {
      this.executorFactory = executorFactory;
      return this;
    }
  }

  public static class Blocking extends ExecutorServiceBinding implements ExecControllerSpec.BlockingSpec {

    public Blocking() {
      this.prefix = "ratpack-blocking";
      this.priority = Thread.MIN_PRIORITY;
    }

    @Override
    public ExecControllerSpec.BlockingSpec prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    @Override
    public ExecControllerSpec.BlockingSpec priority(int priority) {
      this.priority = priority;
      return this;
    }

    @Override
    public ExecControllerSpec.BlockingSpec executor(Function<ThreadFactory, ExecutorService> executorFactory) {
      this.executorFactory = executorFactory;
      return this;
    }
  }

}
