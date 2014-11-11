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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
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
    Deque<ExecutionSegment> segments = new ConcurrentLinkedDeque<>();
  }

  private static class Stream {
    Queue<Event> events = new ConcurrentLinkedQueue<>();

    private Stream() {
      events.add(new Event());
    }
  }

  private final Deque<Stream> streams = new ConcurrentLinkedDeque<>();
  private final Deque<Stream> suspendedStreams = new ConcurrentLinkedDeque<>();

  private final EventLoop eventLoop;
  private final Action<? super Throwable> onError;
  private final Action<? super Execution> onComplete;

  private final ThreadLocal<ExecutionBacking> threadBinding;
  private final Set<ExecutionBacking> executions;

  private final AtomicInteger streaming = new AtomicInteger();
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
    queueStream().events.add(event);
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

  private Stream queueStream() {
    Stream stream = new Stream();
    streams.add(stream);
    return stream;
  }

  private Stream currentStream() {
    return streams.peek();
  }

  private Event currentEvent() {
    return currentStream().events.peek();
  }

  private ExecutionSegment nextSegment() {
    return currentEvent().segments.peek();
  }

  public class StreamHandle {
    private final Stream stream;

    private StreamHandle(Stream stream) {
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
    StreamHandle handle = new StreamHandle(queueStream());
    currentEvent().segments.add(new StreamSubscribe(handle, runnable));
    drain();
  }

  private void drain() {
    if (this.equals(threadBinding.get())) {
      return;
    }

    if (eventLoop.inEventLoop() && threadBinding.get() == null) {
      if (!hasExecutableSegments()) {
        return;
      }
      try {
        threadBinding.set(this);
        assertNotDone();
        while (true) {
          ExecutionSegment segment = nextSegment();
          if (segment == null) {
            Stream stream = currentStream();
            stream.events.remove(); // event is done

            if (stream.events.isEmpty()) {
              if (suspendedStreams.isEmpty()) {
                done();
                return;
              } else {
                if (streaming.get() < suspendedStreams.size()) {
                  streams.remove();
                  streams.addFirst(suspendedStreams.removeLast());
                } else {
                  break;
                }
              }
            }

          } else {
            Event event = currentEvent();
            event.segments.poll();
            segment.run();
          }
        }
      } finally {
        threadBinding.remove();
      }
    } else {
      eventLoop.execute(this::drain);
    }
  }

  private boolean hasExecutableSegments() {
    return currentEvent() != null;
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
        currentEvent().segments.addFirst(new UserCodeSegment(throwException(errorHandlerException)));
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
        Event event = currentEvent();
        event.segments.clear();
        event.segments.addFirst(new ThrowSegment(e));
      }
    }
  }

  private class StreamSubscribe extends ExecutionSegment {
    private final StreamHandle handle;
    private final Consumer<? super StreamHandle> consumer;

    private StreamSubscribe(StreamHandle handle, Consumer<? super StreamHandle> consumer) {
      this.handle = handle;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      suspendedStreams.add(streams.remove());
      streaming.incrementAndGet();
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
      super.run();
      handle.streamEvent(new ExecutionSegment() {
        @Override
        public void run() {
          streaming.decrementAndGet();
        }
      });
    }
  }

}
