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
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.PlatformDependent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.registry.MutableRegistry;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.RegistrySpec;
import ratpack.registry.internal.SimpleMutableRegistry;
import ratpack.stream.TransformablePublisher;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class DefaultExecution implements Execution {

  public final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  private static final ExecutionErrorListener LOG_UNCAUGHT = (e, t) ->
    DefaultExecution.LOGGER.error("Uncaught execution exception in execution " + e + " :", t);

  public final static FastThreadLocal<DefaultExecution> THREAD_BINDING = new FastThreadLocal<>();

  private ExecStream execStream;

  private final ExecControllerInternal controller;
  private final EventLoop eventLoop;
  private final Action<? super Execution> onComplete;

  private List<AutoCloseable> closeables;

  private final MutableRegistry registry = new SimpleMutableRegistry();

  private List<ExecInterceptor> adhocInterceptors;
  private Iterable<? extends ExecInterceptor> interceptors;

  public DefaultExecution(
    ExecControllerInternal controller,
    EventLoop eventLoop,
    Action<? super RegistrySpec> registryInit,
    Action<? super Execution> action,
    Action<? super Execution> onStart,
    Action<? super Execution> onComplete,
    Iterable<ExecutionErrorListener> errorListeners
    ) throws Exception {
    this.controller = controller;
    this.eventLoop = eventLoop;
    this.onComplete = onComplete;

    registryInit.execute(registry);
    errorListeners.forEach(el -> registry.add(ExecutionErrorListener.class, el));
    onStart.execute(this);

    this.execStream = new InitialExecStream(action);

    this.interceptors = Iterables.concat(
      controller.getInterceptors(),
      ImmutableList.copyOf(registry.getAll(ExecInterceptor.class))
    );

    for (ExecInitializer initializer : controller.getInitializers()) {
      initializer.init(this);
    }
    for (ExecInitializer initializer : registry.getAll(ExecInitializer.class)) {
      initializer.init(this);
    }

    drain();
  }

  public static DefaultExecution get() throws UnmanagedThreadException {
    return THREAD_BINDING.get();
  }

  public static DefaultExecution require() throws UnmanagedThreadException {
    DefaultExecution executionBacking = get();
    if (executionBacking == null) {
      throw new UnmanagedThreadException();
    } else {
      return executionBacking;
    }
  }

  public static <T> TransformablePublisher<T> stream(Publisher<T> publisher, Action<? super T> disposer) {
    return publisher instanceof ExecutionBoundPublisher ? (TransformablePublisher<T>) publisher : new ExecutionBoundPublisher<>(publisher, disposer);
  }

  public static <T> Upstream<T> upstream(Upstream<T> upstream) {
    return downstream -> {
      final AtomicBoolean fired = new AtomicBoolean();
      require().delimit(downstream::error, continuation -> {
          try {
            upstream.connect(new Downstream<T>() {
              @Override
              public void error(Throwable throwable) {
                if (!fired.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled", throwable));
                  return;
                }
                continuation.resume(() -> downstream.error(throwable));
              }

              @Override
              public void success(T value) {
                if (!fired.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
                  return;
                }
                continuation.resume(() -> downstream.success(value));
              }

              @Override
              public void complete() {
                if (!fired.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
                  return;
                }
                continuation.resume(downstream::complete);
              }
            });
          } catch (Throwable throwable) {
            if (!fired.compareAndSet(false, true)) {
              LOGGER.error("", new OverlappingExecutionException("promise already fulfilled", throwable));
              return;
            }
            continuation.resume(() -> downstream.error(throwable));
          }
        }
      );
    };
  }

  public EventLoop getEventLoop() {
    return eventLoop;
  }

  public void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment) {
    execStream.enqueue(() -> execStream = new SingleEventExecStream(execStream, onError, segment));
    drain();
  }

  private void delimitStream(Action<? super Throwable> onError, Action<? super ContinuationStream> segment) {
    execStream.enqueue(() -> execStream = new MultiEventExecStream(execStream, onError, segment));
    drain();
  }

  public void eventLoopDrain() {
    eventLoop.execute(this::drain);
  }

  private void drain() {
    if (execStream == TerminalExecStream.INSTANCE || execStream == InErrorHandlerExecStream.INSTANCE) {
      return;
    }

    DefaultExecution currentExecution = THREAD_BINDING.get();
    if (this == currentExecution) {
      return;
    }

    if (!eventLoop.inEventLoop() || currentExecution != null) {
      eventLoopDrain();
      return;
    }

    try {
      THREAD_BINDING.set(this);
      intercept(interceptors.iterator());
    } catch (Throwable e) {
      interceptorError(e);
    } finally {
      THREAD_BINDING.remove();
    }
  }

  public static void interceptorError(Throwable e) {
    LOGGER.warn("exception was thrown by an execution interceptor (which will be ignored):", e);
  }

  public Iterable<? extends ExecInterceptor> getAllInterceptors() {
    return interceptors;
  }

  private void intercept(final Iterator<? extends ExecInterceptor> interceptors) throws Exception {
    if (interceptors.hasNext()) {
      interceptors.next().intercept(this, ExecInterceptor.ExecType.COMPUTE, () -> intercept(interceptors));
    } else {
      exec();
    }
  }

  private void exec() {
    while (true) {
      try {
        if (!(execStream.exec())) {
          break;
        }
      } catch (Throwable segmentError) {
        execStream.error(segmentError);
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
            LOGGER.warn("exception raised by execution closeable " + closeable, e);
          }
        }
      }
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
  public <O> Execution addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    registry.addLazy(type, supplier);
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

    abstract void enqueue(Block block);

    abstract void error(Throwable throwable);
  }

  private static class InErrorHandlerExecStream extends ExecStream {
    static final ExecStream INSTANCE = new InErrorHandlerExecStream();

    @Override
    boolean exec() {
      return false;
    }

    @Override
    void enqueue(Block segment) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }

    @Override
    void error(Throwable throwable) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }
  }

  private static class TerminalExecStream extends ExecStream {

    static final ExecStream INSTANCE = new TerminalExecStream();

    private TerminalExecStream() {
    }

    @Override
    boolean exec() {
      return false;
    }

    @Override
    void enqueue(Block segment) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }

    @Override
    void error(Throwable throwable) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }
  }

  private static class ExecutionBoundPublisher<T> implements TransformablePublisher<T> {

    private final Publisher<T> publisher;
    private final Action<? super T> disposer;

    private ExecutionBoundPublisher(Publisher<T> publisher, Action<? super T> disposer) {
      this.publisher = publisher;
      this.disposer = disposer;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
      require().delimitStream(subscriber::onError, continuation ->
        publisher.subscribe(new Subscriber<T>() {

          private final AtomicBoolean cancelled = new AtomicBoolean();

          @Override
          public void onSubscribe(final Subscription subscription) {
            continuation.event(() ->
              subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                  subscription.request(n);
                }

                @Override
                public void cancel() {
                  cancelled.set(true);
                  subscription.cancel();
                  continuation.complete(Block.noop());
                }
              })
            );
          }

          @Override
          public void onNext(final T element) {
            boolean added = continuation.event(() -> {
              if (cancelled.get()) {
                dispose(element);
              } else {
                subscriber.onNext(element);
              }
            });
            if (!added) {
              dispose(element);
            }
          }

          private void dispose(T element) {
            try {
              disposer.execute(element);
            } catch (Exception e) {
              LOGGER.warn("Exception raised disposing stream item will be ignored - ", e);
            }
          }

          @Override
          public void onComplete() {
            continuation.complete(() -> {
              if (!cancelled.get()) {
                subscriber.onComplete();
              }
            });
          }

          @Override
          public void onError(final Throwable cause) {
            if (!cancelled.get()) {
              continuation.complete(() -> subscriber.onError(cause));
            }
          }
        })
      );
    }
  }

  private class InitialExecStream extends ExecStream {
    Action<? super Execution> initial;
    Queue<Block> segments;

    InitialExecStream(Action<? super Execution> initial) {
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
        initial.execute(DefaultExecution.this);
        initial = null;
        return true;
      }
    }

    @Override
    void enqueue(Block segment) {
      if (segments == null) {
        segments = new ArrayDeque<>(1);
      }
      segments.add(segment);
    }

    @Override
    void error(Throwable throwable) {
      execStream = InErrorHandlerExecStream.INSTANCE;

      initial = null;
      if (segments != null) {
        segments.clear();
      }

      final Execution currentExecution = DefaultExecution.this;
      List<ExecutionErrorListener> errorListeners =
        Lists.newArrayList(registry.getAll(ExecutionErrorListener.class));

      controller.fork()
        .onComplete(e -> {
          execStream = TerminalExecStream.INSTANCE;
          DefaultExecution.this.exec();
        })
        .start(e -> {
          if (errorListeners.size() == 0) {
            errorListeners.add(LOG_UNCAUGHT);
          }
          errorListeners.forEach(errorListener -> {
            try {
              errorListener.onError(currentExecution, throwable);
            } catch (Throwable errorListenerError) {
              LOGGER.error("error listener " + errorListener + " threw error: ", errorListenerError);
            }
          });
        });
    }
  }

  private class SingleEventExecStream extends ExecStream implements Continuation {
    final ExecStream parent;

    private final Action<? super Throwable> onError;
    Action<? super Continuation> initial;
    Block resume;
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
        if (segments == null || segments.isEmpty()) {
          if (resume == null) {
            if (resumed) {
              execStream = parent;
              return true;
            } else {
              return false;
            }
          } else {
            resume.execute();
            resume = null;
            return true;
          }
        } else {
          Block segment = segments.poll();
          if (segment == null) {
            execStream = parent;
            return true;
          } else {
            segment.execute();
            return true;
          }
        }
      } else {
        initial.execute(this);
        initial = null;
        return true;
      }
    }

    @Override
    void enqueue(Block segment) {
      if (segments == null) {
        segments = new ArrayDeque<>(1);
      }
      segments.add(segment);
    }

    public void resume(Block action) {
      resumed = true;
      resume = action;
      drain();
    }

    @Override
    void error(Throwable throwable) {
      execStream = parent;
      if (resumed && resume == null) {
        parent.error(throwable);
      } else {
        try {
          onError.execute(throwable);
        } catch (Throwable e) {
          execStream.error(e);
        }
      }
    }
  }

  private class MultiEventExecStream extends ExecStream implements ContinuationStream {
    final ExecStream parent;
    private final Action<? super Throwable> onError;
    final Queue<Queue<Block>> events = PlatformDependent.newMpscQueue();
    private final AtomicReference<Block> complete = new AtomicReference<>();

    MultiEventExecStream(ExecStream parent, Action<? super Throwable> onError, Action<? super ContinuationStream> initial) {
      this.parent = parent;
      this.onError = onError;
      event(() -> initial.execute(this));
    }

    public boolean event(Block action) {
      if (complete.get() == null) {
        Queue<Block> event = new ArrayDeque<>();
        event.add(action);
        events.add(event);
        drain();
        return true;
      } else {
        return false;
      }
    }

    public boolean complete(Block action) {
      if (complete.compareAndSet(null, action)) {
        drain();
        return true;
      } else {
        return false;
      }
    }

    @Override
    boolean exec() throws Exception {
      Block nextSegment = events.peek().poll();
      if (nextSegment == null) {
        if (events.size() == 1) {
          if (complete.get() == null) {
            return false;
          } else {
            execStream = parent;
            complete.get().execute();
            return true;
          }
        } else {
          events.poll();
          return true;
        }
      } else {
        nextSegment.execute();
        return true;
      }
    }

    @Override
    void enqueue(Block segment) {
      events.peek().add(segment);
    }

    @Override
    void error(Throwable throwable) {
      execStream = parent;
      try {
        onError.execute(throwable);
      } catch (Exception e) {
        execStream.error(e);
      }
    }
  }
}
