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
import io.netty.channel.EventLoop;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.registry.RegistrySpec;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionBacking {

  final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  public final static ThreadLocal<ExecutionBacking> THREAD_BINDING = new ThreadLocal<>();

  private final List<ExecInterceptor> adhocInterceptors = new ArrayList<>(0);
  private final Iterable<? extends ExecInterceptor> interceptors;

  // The “stream” must be a concurrent safe collection because stream events can arrive from other threads
  // All other collections do not need to be concurrent safe because they are only accessed on the event loop
  StreamHandle streamHandle;

  private final EventLoop eventLoop;
  private final List<AutoCloseable> closeables = Lists.newArrayList();
  private final Action<? super Throwable> onError;
  private final Action<? super Execution> onComplete;

  private volatile boolean done;
  private final Execution execution;

  public ExecutionBacking(
    ExecControllerInternal controller,
    EventLoop eventLoop,
    Action<? super RegistrySpec> registry,
    Action<? super Execution> action,
    Action<? super Throwable> onError,
    Action<? super Execution> onStart,
    Action<? super Execution> onComplete
  ) throws Exception {
    this.eventLoop = eventLoop;
    this.onError = onError;
    this.onComplete = onComplete;
    this.execution = new DefaultExecution(this, eventLoop, controller, closeables);

    registry.execute(execution);
    onStart.execute(execution);

    streamHandle = new InitialStreamHandle();
    Deque<Block> event = new ArrayDeque<>();
    //noinspection RedundantCast
    event.add((UserCode) () -> action.execute(execution));
    streamHandle.stream.add(event);

    this.interceptors = Iterables.concat(
      controller.getInterceptors(),
      ImmutableList.copyOf(execution.getAll(ExecInterceptor.class)),
      adhocInterceptors
    );

    for (ExecInitializer initializer : controller.getInitializers()) {
      initializer.init(execution);
    }
    for (ExecInitializer initializer : execution.getAll(ExecInitializer.class)) {
      initializer.init(execution);
    }

    drain();
  }

  private class InitialStreamHandle extends StreamHandle {
    public InitialStreamHandle() {
      super(null);
    }
  }

  public static ExecutionBacking get() throws UnmanagedThreadException {
    return THREAD_BINDING.get();
  }

  public static ExecutionBacking require() throws UnmanagedThreadException {
    ExecutionBacking executionBacking = get();
    if (executionBacking == null) {
      throw new UnmanagedThreadException();
    } else {
      return executionBacking;
    }
  }

  public static <T> TransformablePublisher<T> stream(Publisher<T> publisher) {
    return Streams.transformable(subscriber -> require().streamSubscribe((handle) ->
        publisher.subscribe(new Subscriber<T>() {
          @Override
          public void onSubscribe(final Subscription subscription) {
            handle.event(() ->
                subscriber.onSubscribe(subscription)
            );
          }

          @Override
          public void onNext(final T element) {
            handle.event(() -> subscriber.onNext(element));
          }

          @Override
          public void onComplete() {
            handle.complete(subscriber::onComplete);
          }

          @Override
          public void onError(final Throwable cause) {
            handle.complete(() -> subscriber.onError(cause));
          }
        })
    ));
  }

  public static <T> Upstream<T> upstream(Upstream<T> upstream) {
    return downstream -> {
      final AtomicBoolean fired = new AtomicBoolean();
      require().streamSubscribe(handle -> {
          try {
            upstream.connect(new Downstream<T>() {
              @Override
              public void error(Throwable throwable) {
                if (!fired.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled", throwable));
                  return;
                }
                handle.complete(() -> downstream.error(throwable));
              }

              @Override
              public void success(T value) {
                if (!fired.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
                  return;
                }
                handle.complete(() -> downstream.success(value));
              }

              @Override
              public void complete() {
                if (!fired.compareAndSet(false, true)) {
                  LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
                  return;
                }
                handle.complete(downstream::complete);
              }
            });
          } catch (Throwable throwable) {
            if (!fired.compareAndSet(false, true)) {
              LOGGER.error("", new OverlappingExecutionException("promise already fulfilled", throwable));
              return;
            }
            handle.complete(() -> downstream.error(throwable));

          }
        }
      );
    };
  }

  // Marker interface used to detect user code vs infrastructure code, for error handling and interception
  public interface UserCode extends Block {
  }

  public Execution getExecution() {
    return execution;
  }

  public EventLoop getEventLoop() {
    return eventLoop;
  }

  public void addInterceptor(ExecInterceptor interceptor) {
    adhocInterceptors.add(interceptor);
  }

  public class StreamHandle {
    final StreamHandle parent;
    final Queue<Deque<Block>> stream = new ConcurrentLinkedQueue<>();

    private StreamHandle(StreamHandle parent) {
      this.parent = parent;
      stream.add(new ArrayDeque<>());
    }

    public void event(UserCode action) {
      streamEvent(action);
    }

    public void complete(UserCode action) {
      //noinspection RedundantCast
      streamEvent((UserCode) () -> {
        streamHandle = parent;
        action.execute();
      });
    }

    public void complete() {
      streamEvent(() -> streamHandle = parent);
    }

    private void streamEvent(Block s) {
      Deque<Block> event = new ArrayDeque<>();
      event.add(s);
      stream.add(event);
      drain();
    }
  }

  public void streamSubscribe(Action<? super StreamHandle> consumer) {
    if (done) {
      throw new ExecutionException("this execution has completed (you may be trying to use a promise in a cleanup method)");
    }

    if (streamHandle.stream.isEmpty()) {
      streamHandle.stream.add(new ArrayDeque<>());
    }

    streamHandle.stream.element().add(() -> {
      StreamHandle parent = this.streamHandle;
      this.streamHandle = new StreamHandle(parent);
      consumer.execute(this.streamHandle);
    });

    drain();
  }

  public void eventLoopDrain() {
    eventLoop.execute(this::drain);
  }

  private void drain() {
    if (done) {
      return;
    }

    ExecutionBacking threadBoundExecutionBacking = THREAD_BINDING.get();
    if (this.equals(threadBoundExecutionBacking)) {
      return;
    }

    if (!eventLoop.inEventLoop() || threadBoundExecutionBacking != null) {
      if (!done) {
        eventLoop.execute(this::drain);
      }
      return;
    }

    try {
      THREAD_BINDING.set(this);
      while (true) {
        if (streamHandle.stream.isEmpty()) {
          return;
        }

        Block segment = streamHandle.stream.element().poll();
        if (segment == null) {
          streamHandle.stream.remove();
          if (streamHandle.stream.isEmpty()) {
            if (streamHandle.getClass().equals(InitialStreamHandle.class)) {
              done();
              return;
            } else {
              break;
            }
          }
        } else {
          if (segment instanceof UserCode) {
            try {
              intercept(ExecInterceptor.ExecType.COMPUTE, segment);
            } catch (final Throwable e) {
              Deque<Block> event = streamHandle.stream.element();
              event.clear();
              event.addFirst(() -> {
                try {
                  onError.execute(e);
                } catch (final Throwable errorHandlerException) {
                  //noinspection RedundantCast
                  streamHandle.stream.element().addFirst((UserCode) () -> {
                    throw errorHandlerException;
                  });
                }
              });
            }
          } else {
            try {
              segment.execute();
            } catch (Exception e) {
              LOGGER.error("Internal Ratpack Error - please raise an issue", e);
            }
          }
        }
      }
    } finally {
      THREAD_BINDING.remove();
    }
  }

  private void intercept(ExecInterceptor.ExecType execType, Block segment) throws Exception {
    Iterator<? extends ExecInterceptor> iterator = getAllInterceptors().iterator();
    intercept(execType, iterator, segment);
  }

  public Iterable<? extends ExecInterceptor> getAllInterceptors() {
    return interceptors;
  }

  private void done() {
    done = true;
    try {
      onComplete.execute(getExecution());
    } catch (Throwable e) {
      LOGGER.warn("exception raised during onComplete action", e);
    }

    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Throwable e) {
        LOGGER.warn(String.format("exception raised by closeable %s", closeable), e);
      }
    }
  }

  public void intercept(final ExecInterceptor.ExecType execType, final Iterator<? extends ExecInterceptor> interceptors, Block action) throws Exception {
    if (interceptors.hasNext()) {
      interceptors.next().intercept(execution, execType, () -> intercept(execType, interceptors, action));
    } else {
      action.execute();
    }
  }

}
