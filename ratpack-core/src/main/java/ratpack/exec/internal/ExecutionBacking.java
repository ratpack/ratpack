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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.ExecController;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.exec.ExecutionException;
import ratpack.func.Action;
import ratpack.handling.internal.InterceptedOperation;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionBacking {

  private final static Logger LOGGER = LoggerFactory.getLogger(ExecutionBacking.class);

  private final List<ExecInterceptor> interceptors = new LinkedList<>();
  private final List<AutoCloseable> closeables = new LinkedList<>();
  private final Deque<Runnable> segments = new ConcurrentLinkedDeque<>();
  private final ExecController controller;
  private final Action<? super Throwable> onError;
  private final Action<? super Execution> onComplete;
  private final ThreadLocal<ExecutionBacking> threadBinding;

  private final AtomicBoolean active = new AtomicBoolean();
  private boolean streaming;
  private boolean waiting;
  private boolean done;

  private final Execution execution;

  public ExecutionBacking(ExecController controller, ThreadLocal<ExecutionBacking> threadBinding, Action<? super Execution> action, Action<? super Throwable> onError, Action<? super Execution> onComplete) {
    this.controller = controller;
    this.onError = onError;
    this.onComplete = onComplete;
    this.threadBinding = threadBinding;
    this.execution = new DefaultExecution(controller, closeables);

    segments.addLast(new UserCodeSegment(action));
    drain();
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
    waiting = false;
    drain();
  }

  public void continueVia(final Runnable runnable) {
    segments.addLast(new Runnable() {
      @Override
      public void run() {
        waiting = true;
        runnable.run();
      }
    });
  }

  public void streamExecution(final Action<? super Execution> action) {
    segments.add(new UserCodeSegment(action));
    streaming = true;
    drain();
  }

  public void completeStreamExecution(final Action<? super Execution> action) {
    segments.addLast(new UserCodeSegment(action));
    streaming = false;
    drain();
  }

  private void drain() {
    assertNotDone();
    if (!waiting && !segments.isEmpty()) {
      if (active.compareAndSet(false, true)) {
        if (controller.isManagedThread()) {
          threadBinding.set(this);
          try {
            Runnable segment = segments.poll();
            while (segment != null) {
              segment.run();
              if (waiting) { // the segment initiated an async op
                break;
              } else {
                segment = segments.poll();
                if (segment == null && !streaming) { // not waiting, not streaming and no more segments, we are done
                  done();
                }
              }
            }
          } finally {
            threadBinding.remove();
            active.set(false);
          }
          if (waiting) {
            drain();
          }
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
    } catch (Exception e) {
      LOGGER.warn("exception raised during onComplete action", e);
    }

    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception e) {
        LOGGER.warn(String.format("exception raised by closeable %s", closeable), e);
      }
    }
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
