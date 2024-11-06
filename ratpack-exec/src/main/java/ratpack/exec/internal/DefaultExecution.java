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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import io.netty.channel.EventLoop;
import io.netty.util.internal.PlatformDependent;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.api.Nullable;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.func.Factory;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.registry.internal.DefaultMutableRegistry;
import ratpack.stream.TransformablePublisher;

import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class DefaultExecution implements Execution {

  public final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  private ExecStream execStream;

  private final Ref ref;
  private final ExecController controller;
  private final EventLoop eventLoop;
  private final Action<? super Throwable> onError;
  private final Action<? super Execution> onComplete;

  private List<AutoCloseable> closeables;

  private final MutableRegistry registry = new DefaultMutableRegistry();

  private List<ExecInterceptor> adhocInterceptors;
  private Iterable<? extends ExecInterceptor> interceptors;

  private volatile Thread thread;
  private static final AtomicIntegerFieldUpdater<DefaultExecution> PENDING_INTERRUPT_UPDATER = AtomicIntegerFieldUpdater.newUpdater(DefaultExecution.class, "pendingInterrupt");
  private volatile int pendingInterrupt;

  public DefaultExecution(
    ExecController controller,
    @Nullable ExecutionRef parent,
    EventLoop eventLoop,
    Action<? super RegistrySpec> registryInit,
    Action<? super Execution> action,
    Action<? super Throwable> onError,
    Action<? super Execution> onStart,
    Action<? super Execution> onComplete
  ) throws Exception {
    this.ref = new Ref(this, parent);
    this.controller = controller;
    this.eventLoop = eventLoop;
    this.onError = onError;
    this.onComplete = onComplete;

    registryInit.execute(registry);
    onStart.execute(this);

    this.execStream = new InitialExecStream(() -> action.execute(this));
    this.interceptors = copyInterceptors(controller, registry);

    controller.getInitializers().forEach(initializer -> initializer.init(this));
    registry.getAll(ExecInitializer.class).forEach(initializer -> initializer.init(this));
  }

  private static Iterable<? extends ExecInterceptor> copyInterceptors(ExecController controller, Registry registry) {
    ImmutableList<ExecInterceptor> registryInterceptors = ImmutableList.copyOf(registry.getAll(ExecInterceptor.class));
    if (registryInterceptors.isEmpty()) {
      return controller.getInterceptors();
    } else {
      return Iterables.concat(
        controller.getInterceptors(),
        registryInterceptors
      );
    }
  }

  public static DefaultExecution get() {
    ExecThreadBinding execThreadBinding = ExecThreadBinding.get();
    if (execThreadBinding == null) {
      return null;
    } else {
      return execThreadBinding.getExecution();
    }
  }

  public static DefaultExecution require() throws UnmanagedThreadException {
    DefaultExecution execution = ExecThreadBinding.require().getExecution();
    if (execution == null) {
      throw new IllegalStateException("No execution bound for thread " + Thread.currentThread().getName());
    } else {
      return execution;
    }
  }

  public static <T> TransformablePublisher<T> stream(Publisher<T> publisher, Action<? super T> disposer) {
    return publisher instanceof ExecutionBoundPublisher ? (TransformablePublisher<T>) publisher : new ExecutionBoundPublisher<>(publisher, disposer);
  }

  public static void throwError(Throwable throwable) {
    require().delimit(Action.throwException(), h -> h.resume(Block.throwException(throwable)));
  }

  @Override
  public ExecutionRef getRef() {
    return ref;
  }

  @Override
  public EventLoop getEventLoop() {
    return eventLoop;
  }

  public void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment) {
    execStream.delimit(onError, segment);
    drain();
  }

  public void delimitStream(Action<? super Throwable> onError, Action<? super ContinuationStream> segment) {
    execStream.delimitStream(onError, segment);
    drain();
  }

  public void error(Throwable throwable) {
    execStream.error(throwable);
    drain();
  }

  public void eventLoopDrain() {
    eventLoop.execute(this::drain);
  }

  public boolean isBound() {
    return thread == Thread.currentThread();
  }

  public void drain() {
    if (isBound()) {
      return; // already draining
    }

    if (execStream == TerminalExecStream.INSTANCE) {
      return;
    }

    // Move to the right thread if necessary
    if (!eventLoop.inEventLoop()) {
      eventLoopDrain();
      return;
    }

    // Queue if our thread is busy with another execution
    if (get() != null) {
      eventLoopDrain();
      return;
    }

    try {
      bindToThread();
      exec(interceptors.iterator());
    } catch (Throwable e) {
      interceptorError(e);
    } finally {
      unbindFromThread();
    }
  }

  public void unbindFromThread() {
    thread = null;
    ExecThreadBinding.require().setExecution(null);
    if (Thread.interrupted()) {
      interrupt();
    }
  }

  public void bindToThread() {
    thread = Thread.currentThread();
    if (PENDING_INTERRUPT_UPDATER.compareAndSet(this, 1, 0)) {
      thread.interrupt();
    }
    ExecThreadBinding.require().setExecution(this);
  }

  public <T> T runSync(Factory<T> factory) throws Exception {
    if (isBound()) {
      return factory.create();
    }

    if (ExecThreadBinding.require().getExecution() != null) {
      throw new IllegalStateException("execution already bound");
    }
    class Work implements Block {
      T result;

      @Override
      public void execute() throws Exception {
        result = factory.create();
      }
    }
    Work work = new Work();
    bindToThread();
    try {
      intercept(this, ExecInterceptor.ExecType.COMPUTE, interceptors.iterator(), work);
    } finally {
      unbindFromThread();
    }
    return work.result;
  }

  public static void intercept(Execution execution, ExecInterceptor.ExecType execType, final Iterator<? extends ExecInterceptor> interceptors, Block runnable) throws Exception {
    if (interceptors.hasNext()) {
      interceptors.next().intercept(execution, execType, () -> intercept(execution, execType, interceptors, runnable));
    } else {
      runnable.execute();
    }
  }

  public static void interceptorError(Throwable e) {
    LOGGER.warn("exception was thrown by an execution interceptor (which will be ignored):", e);
  }

  public Iterable<? extends ExecInterceptor> getAllInterceptors() {
    return interceptors;
  }

  private void exec(final Iterator<? extends ExecInterceptor> interceptors) throws Exception {
    if (interceptors.hasNext()) {
      interceptors.next().intercept(this, ExecInterceptor.ExecType.COMPUTE, () -> exec(interceptors));
    } else {
      exec();
    }
  }

  private void exec() {
    while (true) {
      if (Thread.interrupted()) {
        execStream.error(new InterruptedException());
      } else {
        try {
          if (!(execStream.exec())) {
            break;
          }
        } catch (Throwable segmentError) {
          execStream.error(segmentError);
        }
      }
    }

    if (execStream == TerminalExecStream.INSTANCE) {
      try {
        onComplete.execute(this);
      } catch (Throwable e) {
        LOGGER.warn("exception raised during onComplete action", e);
      }

      if (closeables != null) {
        for (AutoCloseable closeable : closeables) {
          try {
            closeable.close();
          } catch (Throwable e) {
            LOGGER.warn("exception raised by execution closeable {}", closeable, e);
          }
        }
      }

      ref.execution = null;
    }
  }

  @Override
  public ExecController getController() {
    return controller;
  }

  @Override
  public void onComplete(AutoCloseable closeable) {
    if (closeables == null) {
      closeables = Lists.newArrayList();
    }
    closeables.add(closeable);
  }

  @Override
  public boolean isComplete() {
    return execStream == TerminalExecStream.INSTANCE;
  }

  @Override
  public <O> Execution addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    registry.addLazy(type, supplier);
    return this;
  }

  @Override
  public <O> Execution add(TypeToken<O> type, O object) {
    registry.add(type, object);
    return this;
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, Block continuation) throws Exception {
    if (adhocInterceptors == null) {
      adhocInterceptors = Lists.newArrayList();
      interceptors = Iterables.concat(interceptors, adhocInterceptors);
    }
    adhocInterceptors.add(execInterceptor);
    execInterceptor.intercept(this, ExecInterceptor.ExecType.COMPUTE, continuation);
  }

  @Override
  public void interrupt() {
    Thread thread = this.thread;
    if (thread == null) {
      PENDING_INTERRUPT_UPDATER.set(this, 1);
      drain();
    } else {
      thread.interrupt();
    }
  }

  @Override
  public <T> void remove(TypeToken<T> type) throws NotInRegistryException {
    registry.remove(type);
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return registry.maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return registry.getAll(type);
  }

  private abstract static class ExecStream {
    abstract boolean exec() throws Exception;

    abstract void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment);

    abstract void delimitStream(Action<? super Throwable> onError, Action<? super ContinuationStream> segment);

    abstract void enqueue(Block segment);

    abstract void error(Throwable throwable);

    abstract ExecStream asParent();
  }

  private abstract class BaseExecStream extends ExecStream {
    void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment) {
      enqueue(() -> execStream = new SingleEventExecStream(execStream.asParent(), onError, segment));
    }

    void delimitStream(Action<? super Throwable> onError, Action<? super ContinuationStream> segment) {
      enqueue(() -> execStream = new MultiEventExecStream(execStream.asParent(), onError, segment));
    }

    @Override
    ExecStream asParent() {
      return this;
    }

  }

  private class NonUserCodeExecStream extends ExecStream {

    protected final ExecStream parent;

    NonUserCodeExecStream(ExecStream parent) {
      this.parent = parent;
    }

    @Override
    boolean exec() {
      execStream = parent;
      return true;
    }

    @Override
    void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment) {
      parent.delimit(onError, segment);
    }

    @Override
    void delimitStream(Action<? super Throwable> onError, Action<? super ContinuationStream> segment) {
      parent.delimitStream(onError, segment);
    }

    @Override
    void enqueue(Block segment) {
      parent.enqueue(segment);
    }

    @Override
    void error(Throwable throwable) {
      parent.error(throwable);
    }

    @Override
    ExecStream asParent() {
      return this;
    }
  }

  private static class TerminalExecStream extends ExecStream {

    private static final ExecStream INSTANCE = new TerminalExecStream();

    private TerminalExecStream() {
    }

    @Override
    boolean exec() {
      return false;
    }

    @Override
    void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }

    @Override
    void delimitStream(Action<? super Throwable> onError, Action<? super ContinuationStream> segment) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }

    @Override
    void enqueue(Block segment) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }

    @Override
    void error(Throwable throwable) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }

    @Override
    ExecStream asParent() {
      return this;
    }
  }

  private class InitialExecStream extends BaseExecStream {
    Block initial;
    Queue<Block> segments;

    InitialExecStream(Block initial) {
      this.initial = initial;
    }

    @Override
    boolean exec() throws Exception {
      if (initial == null) {
        if (segments == null) {
          execStream = TerminalExecStream.INSTANCE;
          return false;
        } else {
          Block segment = segments.poll();
          if (segment == null) {
            execStream = TerminalExecStream.INSTANCE;
            return false;
          } else {
            segment.execute();
            return true;
          }
        }
      } else {
        Block initial = this.initial;
        this.initial = null;
        initial.execute();
        return true;
      }
    }

    @Override
    void enqueue(Block segment) {
      if (initial == null) {
        initial = segment;
      } else {
        if (segments == null) {
          segments = new ArrayDeque<>(1);
        }
        segments.add(segment);
      }
    }

    @Override
    void error(Throwable throwable) {
      initial = null;
      if (segments != null) {
        segments.clear();
      }
      try {
        onError.execute(throwable);
      } catch (Throwable errorHandlerError) {
        LOGGER.error("error handler {} threw error (this execution will terminate):", onError, errorHandlerError);
        execStream = TerminalExecStream.INSTANCE;
      }
    }
  }

  private class SingleEventExecStream extends BaseExecStream implements Continuation {

    final ExecStream parent;

    Action<? super Continuation> initial;
    Action<? super Throwable> onError;

    Action<? super Continuation> next;
    Action<? super Throwable> nextOnError;

    Block resumer;
    boolean resumed;

    Queue<Block> segments;

    SingleEventExecStream(ExecStream parent, Action<? super Throwable> onError, Action<? super Continuation> initial) {
      this.parent = parent;
      this.onError = onError;
      this.initial = initial;
    }

    @Override
    boolean exec() throws Exception {
      if (initial == null) {
        if (next != null) {
          execStream = new SingleEventExecStream(this.asParent(), nextOnError, next);
          next = null;
          return true;
        } else if (segments == null) {
          if (resumer == null) {
            if (resumed) {
              execStream = parent;
              return true;
            } else {
              return false;
            }
          } else {
            resumer.execute();
            resumer = null;

            // Try to reuse this exec stream instead of branching for another.
            // If the stream only initiates one promise, then this stream is effectively empty at this point.
            // So, we unpack the block and reuse this.
            // This is a dramatic saving for the extremely common case where an execution is a long series of serial promises.
            // We can almost always do this.
            if (next != null) {
              initial = next;
              onError = nextOnError;
              next = null;
              nextOnError = null;
              resumed = false;
            }

            return true;
          }
        } else {
          Block segment = segments.poll();
          if (segments.isEmpty()) {
            segments = null;
          }
          Objects.requireNonNull(segment).execute();
          return true;
        }
      } else {
        initial.execute(this);
        initial = null;
        return true;
      }
    }

    @Override
    ExecStream asParent() {
      // If this segment is done, avoiding holding a reference to it
      return resumed && resumer == null && segments == null ? parent.asParent() : this;
    }

    @Override
    void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment) {
      if (next == null && segments == null) {
        next = segment;
        nextOnError = onError;
      } else {
        if (next != null) {
          super.delimit(nextOnError, next);
          next = null;
          nextOnError = null;
        }
        super.delimit(onError, segment);
      }
    }

    @Override
    void enqueue(Block segment) {
      if (segments == null) {
        segments = new ArrayDeque<>();
      }
      segments.add(segment);
    }

    public void resume(Block action) {
      if (isBound()) {
        doResume(action);
      } else {
        eventLoop.execute(() -> doResume(action));
      }
    }

    private void doResume(Block action) {
      resumed = true;
      resumer = action;
      drain();
    }

    @Override
    void error(Throwable throwable) {
      if (resumed && resumer == null) {
        execStream = parent;
        execStream.error(throwable);
      } else {
        execStream = new SingleEventExecStream(parent, parent::error, continuation -> continuation.resume(() -> onError.execute(throwable)));
        drain();
      }
    }
  }

  private class MultiEventExecStream extends NonUserCodeExecStream {
    final Queue<ExecStream> events = PlatformDependent.newMpscQueue();

    final ExecStream nextEvent = new NonUserCodeExecStream(this);
    private final Action<? super Throwable> onError;

    MultiEventExecStream(ExecStream parent, Action<? super Throwable> onError, Action<? super ContinuationStream> initial) {
      super(parent);
      this.onError = onError;
      events.add(new SingleEventExecStream(nextEvent, Action.throwException(), continuation -> continuation.resume(() ->
        initial.execute(new ContinuationStream() {
          public void event(Block action) {
            addEvent(nextEvent, action);
          }

          @Override
          public boolean isEmpty() {
            return events.isEmpty();
          }

          public void complete(Block action) {
            addEvent(new NonUserCodeExecStream(parent), action);
          }

          private void addEvent(ExecStream parent, Block action) {
            events.add(new SingleEventExecStream(parent, Action.throwException(), continuation -> continuation.resume(action)));
            drain();
          }
        })
      )));
    }

    @Override
    boolean exec() {
      ExecStream next = events.poll();
      if (next == null) {
        return false;
      } else {
        execStream = next;
        return true;
      }
    }

    @Override
    void error(Throwable throwable) {
      execStream = new SingleEventExecStream(parent, parent::error, continuation -> continuation.resume(() -> onError.execute(throwable)));
      drain();
    }
  }

  private static final class Ref implements ExecutionRef {

    private Execution execution;
    private final ExecutionRef parent;

    private Ref(Execution execution, ExecutionRef parent) {
      this.execution = execution;
      this.parent = parent;
    }

    private Registry getRegistry() {
      Execution execution = this.execution;
      if (execution == null) {
        return Registry.empty();
      } else {
        return execution;
      }
    }

    @Override
    public ExecutionRef getParent() {
      if (parent == null) {
        throw new IllegalStateException("execution does not have a parent");
      }
      return parent;
    }

    @Override
    public Optional<ExecutionRef> maybeParent() {
      return Optional.ofNullable(parent);
    }

    @Override
    public boolean isComplete() {
      Execution execution = this.execution;
      return execution == null || execution.isComplete();
    }

    @Override
    public <O> Optional<O> maybeGet(TypeToken<O> type) {
      return getRegistry().maybeGet(type);
    }

    @Override
    public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
      return getRegistry().getAll(type);
    }
  }

}
