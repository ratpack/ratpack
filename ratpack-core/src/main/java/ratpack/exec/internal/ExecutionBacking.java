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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static ratpack.func.Action.throwException;

public class ExecutionBacking {

  public static final boolean TRACE = Boolean.getBoolean("ratpack.execution.trace");

  final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  private final long startedAt = System.currentTimeMillis();

  private final List<ExecInterceptor> interceptors = Lists.newLinkedList();
  private final List<AutoCloseable> closeables = Lists.newLinkedList();

  // stream has events, events have segments

  private static class Event {
    Deque<ExecutionSegment> segments = Lists.newLinkedList();
  }

  private static class Stream {
    Queue<Event> events = new ConcurrentLinkedQueue<>();
    Stream innerStream;

    private Stream() {
      events.add(new Event());
    }

    Stream activeStream() {
      return innerStream == null ? this : innerStream.activeStream();
    }
  }

  private final Stream stream = new Stream();

  private final EventLoop eventLoop;
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
    Event event = new Event();
    event.segments.add(new UserCodeSegment(action));
    stream.events.add(event);
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
    final Stream parent;
    final Stream stream;

    private StreamHandle(Stream parent, Stream stream) {
      this.parent = parent;
      this.stream = stream;
    }

    public void event(Action<? super Execution> action) {
      streamEvent(new StreamEvent(action));
    }

    public void complete(Action<? super Execution> action) {
      streamEvent(new StreamCompletion(this, action));
    }

    private void streamEvent(ExecutionSegment s) {
      Event event = new Event();
      event.segments.add(s);
      stream.events.add(event);
      drain();
    }
  }

  public void streamSubscribe(Consumer<? super StreamHandle> runnable) {
    assertNotDone();
    stream.activeStream().events.element().segments.add(new StreamSubscribe(runnable));
    drain();
  }

  private void drain() {
    ExecutionBacking threadBoundExecutionBacking = threadBinding.get();

    if (this.equals(threadBoundExecutionBacking)) {
      return;
    }

    if (!eventLoop.inEventLoop() || threadBoundExecutionBacking != null) {
      eventLoop.execute(this::drain);
      return;
    }

    try {
      threadBinding.set(this);
      while (true) {
        Queue<Event> events = stream.activeStream().events;
        if (events.isEmpty()) {
          return;
        }

        ExecutionSegment segment = events.element().segments.poll();
        if (segment == null) {
          events.remove();
          if (events.isEmpty()) {
            if (stream.events.isEmpty()) {
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
    return !stream.activeStream().events.isEmpty();
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

    executions.remove(this);
  }

  private void assertNotDone() {
    if (done) {
      throw new ExecutionException("execution is complete");
    }
  }

  public void intercept(final ExecInterceptor.ExecType execType, final List<ExecInterceptor> interceptors, final Action<? super Execution> action) throws Exception {
    new InterceptedOperation(execType, interceptors) {
      @Override
      protected void performOperation() throws Exception {
        action.execute(getExecution());
      }
    }.run();
  }

  private abstract class ExecutionSegment implements Runnable {
    private final Optional<StackTraceElement[]> trace;

    protected ExecutionSegment() {
      this.trace = TRACE ? Optional.of(Thread.currentThread().getStackTrace()) : Optional.empty();
    }

    public Optional<StackTraceElement[]> getTrace() {
      return trace;
    }
  }

  private class ThrowSegment extends ExecutionSegment {
    private final Throwable throwable;

    private ThrowSegment(Throwable throwable) {
      this.throwable = throwable;
    }

    @Override
    public void run() {
      try {
        onError.execute(throwable);
      } catch (final Throwable errorHandlerException) {
        stream.activeStream().events.element().segments.addFirst(new UserCodeSegment(throwException(errorHandlerException)));
      }
    }
  }

  private class UserCodeSegment extends ExecutionSegment {
    private final Action<? super Execution> action;

    public UserCodeSegment(Action<? super Execution> action) {
      this.action = action;
    }

    @Override
    public void run() {
      try {
        intercept(ExecInterceptor.ExecType.COMPUTE, interceptors, action);
      } catch (final Throwable e) {
        Event event = stream.activeStream().events.element();
        event.segments.clear();
        event.segments.addFirst(new ThrowSegment(e));
      }
    }
  }

  private class StreamSubscribe extends ExecutionSegment {
    private final Consumer<? super StreamHandle> consumer;

    private StreamSubscribe(Consumer<? super StreamHandle> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void run() {
      Stream activeStream = stream.activeStream();
      activeStream.innerStream = new Stream();
      StreamHandle handle = new StreamHandle(activeStream, activeStream.innerStream);
      consumer.accept(handle);
    }
  }

  private class StreamEvent extends UserCodeSegment {
    private StreamEvent(Action<? super Execution> action) {
      super(action);
    }
  }

  private class StreamCompletion extends UserCodeSegment {
    private final StreamHandle handle;

    private StreamCompletion(StreamHandle handle, Action<? super Execution> action) {
      super(action);
      this.handle = handle;
    }

    @Override
    public void run() {
      handle.parent.innerStream = null;
      super.run();
    }
  }

}
