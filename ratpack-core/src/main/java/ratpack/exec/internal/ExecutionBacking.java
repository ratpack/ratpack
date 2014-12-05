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
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.NoArgAction;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ExecutionBacking {

  public static final boolean TRACE = Boolean.getBoolean("ratpack.execution.trace");

  final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  private final long startedAt = System.currentTimeMillis();

  // package access to allow direct field access by inners
  final List<ExecInterceptor> interceptors = Lists.newLinkedList();

  // The “stream” must be a concurrent safe collection because stream events can arrive from other threads
  // All other collections do not need to be concurrent safe because they are only accessed on the event loop
  Queue<Deque<Runnable>> stream = new ConcurrentLinkedQueue<>();

  private final EventLoop eventLoop;
  private final List<AutoCloseable> closeables = Lists.newLinkedList();
  private final Action<? super Throwable> onError;
  private final Action<? super Execution> onComplete;

  private final ThreadLocal<ExecutionBacking> threadBinding;
  private final Set<ExecutionBacking> executions;

  private volatile boolean done;
  private final Execution execution;

  private Optional<StackTraceElement[]> startTrace = Optional.empty();

  public ExecutionBacking(ExecController controller, Set<ExecutionBacking> executions, EventLoop eventLoop, Optional<StackTraceElement[]> startTrace, ThreadLocal<ExecutionBacking> threadBinding, Action<? super Execution> action, Action<? super Throwable> onError, Action<? super Execution> onComplete) {
    this.executions = executions;
    this.eventLoop = eventLoop;
    this.onError = onError;
    this.onComplete = onComplete;
    this.threadBinding = threadBinding;
    this.execution = new DefaultExecution(eventLoop, controller, closeables);
    this.startTrace = startTrace;

    executions.add(this);

    Deque<Runnable> event = Lists.newLinkedList();
    event.add(() -> execUserCode(() -> action.execute(execution)));
    stream.add(event);

    Deque<Runnable> doneEvent = Lists.newLinkedList();
    doneEvent.add(() -> done = true);
    stream.add(doneEvent);
    drain();
  }

  private class Snapshot implements ExecutionSnapshot {

    private final boolean waiting;

    private Snapshot() {
      this.waiting = !hasExecutableSegments();
    }

    @Override
    public String getId() {
      return Integer.toString(System.identityHashCode(ExecutionBacking.this));
    }

    @Override
    public boolean getWaiting() {
      return waiting;
    }


    @Override
    public Long getStartedAt() {
      return startedAt;
    }

    @Override
    public Optional<StackTraceElement[]> getStartedTrace() {
      return startTrace;
    }
  }

  public ExecutionSnapshot getSnapshot() {
    return new Snapshot();
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
    final Queue<Deque<Runnable>> parent;
    final Queue<Deque<Runnable>> stream;

    private StreamHandle(Queue<Deque<Runnable>> parent, Queue<Deque<Runnable>> stream) {
      this.parent = parent;
      this.stream = stream;
    }

    public void event(NoArgAction action) {
      streamEvent(() -> execUserCode(action));
    }

    public void complete(NoArgAction action) {
      streamEvent(() -> {
        ExecutionBacking.this.stream = this.parent;
        execUserCode(action);
      });
    }

    private void streamEvent(Runnable s) {
      Deque<Runnable> event = Lists.newLinkedList();
      event.add(s);
      stream.add(event);
      drain();
    }
  }

  public void streamSubscribe(Consumer<? super StreamHandle> consumer) {
    stream.element().add(() -> {
      Queue<Deque<Runnable>> parent = stream;
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

        Runnable segment = stream.element().poll();
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
          segment.run();
        }
      }
    } finally {
      threadBinding.remove();
    }
  }

  private boolean hasExecutableSegments() {
    return !stream.isEmpty();
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

    executions.remove(this);
  }

  private void execUserCode(NoArgAction code) {
    try {
      intercept(ExecInterceptor.ExecType.COMPUTE, interceptors, code);
    } catch (final Throwable e) {
      Deque<Runnable> event = stream.element();
      event.clear();
      event.addFirst(() -> {
        try {
          onError.execute(e);
        } catch (final Throwable errorHandlerException) {
          stream.element().addFirst(() -> execUserCode(NoArgAction.throwException(errorHandlerException)));
        }
      });
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
