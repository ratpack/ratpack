/*
 * Copyright 2022 the original author or authors.
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

package ratpack.core.service.scheduled;


import io.netty.channel.EventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.core.service.Service;
import ratpack.core.service.StartEvent;
import ratpack.core.service.StopEvent;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.util.Promised;
import ratpack.func.Action;
import ratpack.func.Exceptions;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A type of {@link Service} which repeatedly does some work according to a provided schedule.
 */
public abstract class RecurringService implements Service {

  /**
   * Defines what this service will do when the application starts.
   */
  protected enum OnStart {
    /**
     * Execute the work immediately (without consuming exceptions), blocking startup, then repeat on schedule.
     */
    INLINE,

    /**
     * Execute the work immediately, without blocking startup, then repeat on schedule.
     */
    EXEC,

    /**
     * Start the schedule.
     */
    NEXT,

    /**
     * Do not start the schedule.
     */
    NONE
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(RecurringService.class);

  private final AtomicReference<ScheduledFuture<?>> scheduledFutureRef = new AtomicReference<>();
  private final AtomicReference<Promised<?>> currentExecutionCompleteRef = new AtomicReference<>();
  private final AtomicReference<Execution> currentExecutionRef = new AtomicReference<>();

  /**
   * Represents the current scheduling state of the {@link RecurringService}.
   */
  protected enum State {
    UNSCHEDULED,
    SCHEDULED,
    RUNNING,
    STOPPED
  }

  private final EventLoop eventLoop;
  private final OnStart onStart;
  private final AtomicReference<State> stateRef = new AtomicReference<>(State.UNSCHEDULED);

  protected RecurringService(ExecController execController, OnStart onStart) {
    this.eventLoop = execController.getEventLoopGroup().next();
    this.onStart = onStart;
  }

  protected abstract void performAction() throws Exception;

  protected abstract Duration nextDelay();

  @Override
  public void onStart(StartEvent event) {
    switch (onStart) {
      case INLINE:
        Operation.of(() -> {
            stateRef.set(State.RUNNING);
            performAction();
          })
          .onError(e -> {
            stateRef.set(State.STOPPED);
            throw Exceptions.uncheck(e);
          })
          .then(() -> {
            stateRef.set(State.UNSCHEDULED);
            scheduleNext();
          });
        break;
      case EXEC:
        scheduleNow();
        break;
      case NEXT:
        scheduleNext();
        break;
      case NONE:
        break;
      default:
        throw new IllegalStateException("Unhandled switch value (" + onStart.getClass().getName() + "):" + onStart);
    }
  }

  @Override
  public void onStop(StopEvent event) {
    stop().then();
  }

  protected void stopRequested() {
  }

  private State requestStop() {
    RecurringService.State state = stateRef.getAndSet(State.STOPPED);
    stopRequested();
    if (state == State.SCHEDULED) {
      ScheduledFuture<?> scheduledFuture = scheduledFutureRef.get();
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
    }
    return state;
  }

  private Operation stop() {
    return Operation.of(() -> {
      if (requestStop() == State.RUNNING && isInAction()) {
        currentExecutionCompleteRef.get().promise().then(Action.noop());
      }
    });
  }

  private boolean isInAction() {
    return Execution.currentOpt()
      .map(e -> e == currentExecutionRef.get())
      .orElse(false);
  }

  private Promised<?> exec() {
    Promised<?> completion = new Promised<>();
    currentExecutionCompleteRef.set(completion);

    Execution.fork()
      .onError(t -> LOGGER.error("An error occurred during recurring job of {}:", getClass().getName(), t))
      .onComplete(e -> {
        currentExecutionCompleteRef.get().complete();
        currentExecutionRef.set(null);
        stateRef.compareAndSet(State.RUNNING, State.UNSCHEDULED);
        scheduleNext();
      })
      .start(e -> {
        currentExecutionRef.set(e);
        if (stateRef.compareAndSet(State.SCHEDULED, State.RUNNING)) {
          performAction();
        }
      });

    return completion;
  }

  private void scheduleNow() {
    RecurringService.State previousState = stateRef.getAndSet(State.SCHEDULED);
    if (previousState == State.UNSCHEDULED) {
      exec();
    } else {
      throw new IllegalStateException("Expected unscheduled state but was: " + previousState);
    }
  }

  protected final void scheduleNext() {
    if (stateRef.compareAndSet(State.UNSCHEDULED, State.SCHEDULED)) {
      ScheduledFuture<?> future = eventLoop.schedule(this::exec, nextDelay().toMillis(), TimeUnit.MILLISECONDS);
      scheduledFutureRef.set(future);
      if (stateRef.get() == State.STOPPED) {
        future.cancel(true);
      }
    }
  }

  protected final void restart() {
    stateRef.compareAndSet(State.STOPPED, State.UNSCHEDULED);
  }

  protected State getState() {
    return stateRef.get();
  }

  public boolean isStopped() {
    return getState() == State.STOPPED;
  }
}
