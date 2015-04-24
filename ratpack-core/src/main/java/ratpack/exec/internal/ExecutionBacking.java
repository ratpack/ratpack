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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.Block;
import ratpack.registry.RegistrySpec;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ExecutionBacking {

  final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  private final static ThreadLocal<ExecutionBacking> THREAD_BINDING = new ThreadLocal<>();

  // package access to allow direct field access by inners
  final ImmutableList<? extends ExecInterceptor> globalInterceptors;
  final ImmutableList<? extends ExecInterceptor> registryInterceptors;
  List<ExecInterceptor> adhocInterceptors;

  // The “stream” must be a concurrent safe collection because stream events can arrive from other threads
  // All other collections do not need to be concurrent safe because they are only accessed on the event loop
  Queue<Deque<Block>> stream = new ConcurrentLinkedQueue<>();

  private final EventLoop eventLoop;
  private final List<AutoCloseable> closeables = Lists.newLinkedList();
  private final BiAction<? super Execution, ? super Throwable> onError;
  private final Action<? super Execution> onComplete;

  private volatile boolean done;
  private final Execution execution;

  public ExecutionBacking(
    ExecController controller,
    EventLoop eventLoop,
    ImmutableList<? extends ExecInterceptor> globalInterceptors,
    Action<? super RegistrySpec> registry,
    Action<? super Execution> action,
    BiAction<? super Execution, ? super Throwable> onError,
    Action<? super Execution> onComplete
  ) throws Exception {
    this.eventLoop = eventLoop;
    this.onError = onError;
    this.onComplete = onComplete;
    this.execution = new DefaultExecution(eventLoop, controller, closeables);

    registry.execute(execution);

    this.registryInterceptors = ImmutableList.copyOf(execution.getAll(ExecInterceptor.class));
    this.globalInterceptors = globalInterceptors;

    Deque<Block> event = Lists.newLinkedList();
    //noinspection RedundantCast
    event.add((UserCode) () -> action.execute(execution));
    stream.add(event);

    Deque<Block> doneEvent = Lists.newLinkedList();
    doneEvent.add(() -> done = true);
    stream.add(doneEvent);
    drain();
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
    if (adhocInterceptors == null) {
      adhocInterceptors = Lists.newArrayList();
    }
    adhocInterceptors.add(interceptor);
  }

  public class StreamHandle {
    final Queue<Deque<Block>> parent;
    final Queue<Deque<Block>> stream;

    private StreamHandle(Queue<Deque<Block>> parent, Queue<Deque<Block>> stream) {
      this.parent = parent;
      this.stream = stream;
    }

    public void event(UserCode action) {
      streamEvent(action);
    }

    public void complete(UserCode action) {
      //noinspection RedundantCast
      streamEvent((UserCode) () -> {
        ExecutionBacking.this.stream = this.parent;
        action.execute();
      });
    }

    public EventLoop getEventLoop() {
      return execution.getEventLoop();
    }

    private void streamEvent(Block s) {
      Deque<Block> event = Lists.newLinkedList();
      event.add(s);
      stream.add(event);
      drain();
    }
  }

  public void streamSubscribe(Consumer<? super StreamHandle> consumer) {
    stream.element().add(() -> {
      Queue<Deque<Block>> parent = stream;
      stream = new ConcurrentLinkedDeque<>();
      stream.add(Lists.newLinkedList());
      StreamHandle handle = new StreamHandle(parent, stream);
      consumer.accept(handle);
    });

    drain();
  }

  private void drain() {
    if (done) {
      throw new ExecutionException("execution is complete");
    }

    ExecutionBacking threadBoundExecutionBacking = THREAD_BINDING.get();
    if (this.equals(threadBoundExecutionBacking)) {
      return;
    }

    if (!eventLoop.inEventLoop() || threadBoundExecutionBacking != null) {
      eventLoop.execute(this::drain);
      return;
    }

    try {
      THREAD_BINDING.set(this);
      while (true) {
        if (stream.isEmpty()) {
          return;
        }

        Block segment = stream.element().poll();
        if (segment == null) {
          stream.remove();
          if (stream.isEmpty()) {
            if (done) {
              done();
              return;
            } else {
              break;
            }
          }
        } else {
          if (segment instanceof UserCode) {
            try {
              intercept(ExecInterceptor.ExecType.COMPUTE, getAllInterceptors().iterator(), segment);
            } catch (final Throwable e) {
              Deque<Block> event = stream.element();
              event.clear();
              event.addFirst(() -> {
                try {
                  onError.execute(execution, e);
                } catch (final Throwable errorHandlerException) {
                  //noinspection RedundantCast
                  stream.element().addFirst((UserCode) () -> {
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

  public Iterable<? extends ExecInterceptor> getAllInterceptors() {
    Iterable<? extends ExecInterceptor> interceptors;
    if (adhocInterceptors == null) {
      interceptors = Iterables.concat(globalInterceptors, registryInterceptors);
    } else {
      interceptors = Iterables.concat(globalInterceptors, registryInterceptors, adhocInterceptors);
    }
    return interceptors;
  }

  private void done() {
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
