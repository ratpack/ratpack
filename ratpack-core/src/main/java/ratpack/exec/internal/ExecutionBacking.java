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

import com.google.common.collect.Lists;
import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.ExecController;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.exec.ExecutionException;
import ratpack.func.Action;
import ratpack.func.NoArgAction;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ExecutionBacking {

  public static final boolean TRACE = Boolean.getBoolean("ratpack.execution.trace");

  final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  // package access to allow direct field access by inners
  final List<ExecInterceptor> interceptors = Lists.newLinkedList();

  // The “stream” must be a concurrent safe collection because stream events can arrive from other threads
  // All other collections do not need to be concurrent safe because they are only accessed on the event loop
  Queue<Deque<NoArgAction>> stream = new ConcurrentLinkedQueue<>();

  private final EventLoop eventLoop;
  private final List<AutoCloseable> closeables = Lists.newLinkedList();
  private final Action<? super Throwable> onError;
  private final Action<? super Execution> onComplete;

  private final ThreadLocal<ExecutionBacking> threadBinding;

  private volatile boolean done;
  private final Execution execution;

  public ExecutionBacking(ExecController controller, EventLoop eventLoop, Optional<StackTraceElement[]> startTrace, ThreadLocal<ExecutionBacking> threadBinding, Action<? super Execution> action, Action<? super Throwable> onError, Action<? super Execution> onComplete) {
    this.eventLoop = eventLoop;
    this.onError = onError;
    this.onComplete = onComplete;
    this.threadBinding = threadBinding;
    this.execution = new DefaultExecution(eventLoop, controller, closeables);

    Deque<NoArgAction> event = Lists.newLinkedList();
    //noinspection RedundantCast
    event.add((UserCode) () -> action.execute(execution));
    stream.add(event);

    Deque<NoArgAction> doneEvent = Lists.newLinkedList();
    doneEvent.add(() -> done = true);
    stream.add(doneEvent);
    drain();
  }

  // Marker interface used to detect user code vs infrastructure code, for error handling and interception
  public interface UserCode extends NoArgAction {
  }

  public Execution getExecution() {
    return execution;
  }

  public EventLoop getEventLoop() {
    return eventLoop;
  }

  public List<ExecInterceptor> getInterceptors() {
    return interceptors;
  }

  public class StreamHandle {
    final Queue<Deque<NoArgAction>> parent;
    final Queue<Deque<NoArgAction>> stream;

    private StreamHandle(Queue<Deque<NoArgAction>> parent, Queue<Deque<NoArgAction>> stream) {
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

    private void streamEvent(NoArgAction s) {
      Deque<NoArgAction> event = Lists.newLinkedList();
      event.add(s);
      stream.add(event);
      drain();
    }
  }

  public void streamSubscribe(Consumer<? super StreamHandle> consumer) {
    stream.element().add(() -> {
      Queue<Deque<NoArgAction>> parent = stream;
      stream = new ConcurrentLinkedDeque<>();
      stream.add(Lists.newLinkedList());
      StreamHandle handle = new StreamHandle(parent, stream);
      consumer.accept(handle);
    });

    drain();
  }

  private void drain() {
    ExecutionBacking threadBoundExecutionBacking = threadBinding.get();
    if (this.equals(threadBoundExecutionBacking)) {
      return;
    }

    if (done) {
      throw new ExecutionException("execution is complete");
    }

    if (!eventLoop.inEventLoop() || threadBoundExecutionBacking != null) {
      eventLoop.execute(this::drain);
      return;
    }

    try {
      threadBinding.set(this);
      while (true) {
        if (stream.isEmpty()) {
          return;
        }

        NoArgAction segment = stream.element().poll();
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
              intercept(ExecInterceptor.ExecType.COMPUTE, interceptors, segment);
            } catch (final Throwable e) {
              Deque<NoArgAction> event = stream.element();
              event.clear();
              event.addFirst(() -> {
                try {
                  onError.execute(e);
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
      threadBinding.remove();
    }
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

  public void intercept(final ExecInterceptor.ExecType execType, final List<ExecInterceptor> interceptors, NoArgAction action) throws Exception {
    new InterceptedOperation(execType, interceptors) {
      @Override
      protected void performOperation() throws Exception {
        action.execute();
      }
    }.run();
  }

}
