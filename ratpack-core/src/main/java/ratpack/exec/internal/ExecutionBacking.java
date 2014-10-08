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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.handling.internal.InterceptedOperation;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ExecutionBacking {

  final static Logger LOGGER = LoggerFactory.getLogger(Execution.class);

  private final List<ExecInterceptor> interceptors = new LinkedList<>();
  private final List<AutoCloseable> closeables = new LinkedList<>();
  private final Deque<Runnable> segments = new ConcurrentLinkedDeque<>();
  private final ExecController controller;
  private final Set<ExecutionBacking> executions;
  private final Action<? super Throwable> onError;
  private final Action<? super Execution> onComplete;
  private final ThreadLocal<ExecutionBacking> threadBinding;
  private final Queue<String> checkpoints = new ConcurrentLinkedQueue<>();

  private final AtomicBoolean active = new AtomicBoolean();
  private final AtomicBoolean streaming = new AtomicBoolean();
  private final AtomicBoolean waiting = new AtomicBoolean();
  private boolean done;
  private final long startedAt = System.currentTimeMillis();

  private final Execution execution;

  private Optional<StackTraceElement[]> startTrace = Optional.empty();
  private AtomicReference<StackTraceElement[]> lastSegmentTrace = new AtomicReference<>();

  public ExecutionBacking(ExecController controller, Set<ExecutionBacking> executions, Optional<StackTraceElement[]> startTrace, ThreadLocal<ExecutionBacking> threadBinding, Action<? super Execution> action, Action<? super Throwable> onError, Action<? super Execution> onComplete) {
    this.controller = controller;
    this.executions = executions;
    this.onError = onError;
    this.onComplete = onComplete;
    this.threadBinding = threadBinding;
    this.execution = new DefaultExecution(controller, closeables, checkpoints);
    this.startTrace = startTrace;

    if (startTrace.isPresent()) {
      interceptors.add((execType, continuation) -> {
        lastSegmentTrace.set(Thread.currentThread().getStackTrace());
        continuation.run();
      });
    }

    segments.addLast(new UserCodeSegment(action));
    executions.add(this);
    drain();
  }

  private class Snapshot implements ExecutionSnapshot {

    private final boolean waiting;
    private final List<String> checkpoints;
    private final Optional<StackTraceElement[]> lastSegmentTrace;

    private Snapshot() {
      this.waiting = ExecutionBacking.this.waiting.get();
      this.checkpoints = Lists.newArrayList(ExecutionBacking.this.checkpoints);
      this.lastSegmentTrace = Optional.ofNullable(ExecutionBacking.this.lastSegmentTrace.get());
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
    public List<String> getCheckpoints() {
      return checkpoints;
    }

    @Override
    public Long getStartedAt() {
      return startedAt;
    }

    @Override
    public Optional<StackTraceElement[]> getStartedTrace() {
      return startTrace;
    }

    @Override
    public Optional<StackTraceElement[]> getLastSegmentTrace() {
      return lastSegmentTrace;
    }
  }

  public ExecutionSnapshot getSnapshot() {
    return new Snapshot();
  }

  public Execution getExecution() {
    return execution;
  }

  public ExecController getController() {
    return controller;
  }

  public List<ExecInterceptor> getInterceptors() {
    return interceptors;
  }

  public void join(final Action<? super Execution> action) {
    segments.addFirst(new UserCodeSegment(action));
    waiting.set(false);
    drain();
  }

  public void continueVia(final Runnable runnable) {
    if (waiting.get()) {
      throw new ExecutionException("Asynchronous actions cannot be initiated while initiating an async action, use a forked execution or promise operations.");
    }
    segments.addLast(() -> {
      waiting.set(true);
      runnable.run();
    });
  }

  public void streamExecution(final Action<? super Execution> action) {
    segments.add(new UserCodeSegment(action));
    streaming.set(true);
    drain();
  }

  public void completeStreamExecution(final Action<? super Execution> action) {
    segments.addLast(new UserCodeSegment(action));
    streaming.set(false);
    drain();
  }

  private void drain() {
    if (!waiting.get() && !segments.isEmpty()) {
      assertNotDone();
      if (active.compareAndSet(false, true)) {
        if (threadBinding.get() == null && controller.isManagedThread()) {
          threadBinding.set(this);
          try {
            Runnable segment = segments.poll();
            while (segment != null) {
              segment.run();
              if (waiting.get()) { // the segment initiated an async op
                break;
              } else {
                segment = segments.poll();
                if (segment == null && !streaming.get()) { // not waiting, not streaming and no more segments, we are done
                  done();
                  return;
                }
              }
            }
          } finally {
            threadBinding.remove();
            active.set(false);
          }
          drain();
        } else {
          active.set(false);
          controller.getEventLoopGroup().submit(this::drain);
        }
      }
    }
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

  private class UserCodeSegment implements Runnable {
    private final Action<? super Execution> action;

    public UserCodeSegment(Action<? super Execution> action) {
      this.action = action;
    }

    @Override
    public void run() {
      try {
        intercept(ExecInterceptor.ExecType.COMPUTE, interceptors, action);
      } catch (final Throwable e) {
        segments.clear();
        segments.addFirst(() -> {
          try {
            onError.execute(e);
          } catch (final Throwable errorHandlerException) {
            segments.addFirst(new UserCodeSegment(Action.throwException(errorHandlerException)));
          }
        });
      }
    }
  }

}
