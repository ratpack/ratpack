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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.registry.RegistrySpec;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static ratpack.func.Action.noop;

public class DefaultExecControl implements ExecControl {

  private static final Logger LOGGER = ExecutionBacking.LOGGER;
  private static final Action<Throwable> LOG_UNCAUGHT = t -> LOGGER.error("Uncaught execution exception", t);
  private static final int MAX_ERRORS_THRESHOLD = 5;

  private final ExecController execController;
  private final ThreadLocal<ExecutionBacking> threadBinding = new ThreadLocal<>();

  public DefaultExecControl(ExecController execController) {
    this.execController = execController;
  }

  private ExecutionBacking getBacking() {
    ExecutionBacking executionBacking = threadBinding.get();
    if (executionBacking == null) {
      throw new ExecutionException("Current thread has no bound execution");
    } else {
      return executionBacking;
    }
  }

  @Override
  public Execution getExecution() {
    return getBacking().getExecution();
  }

  @Override
  public ExecController getController() {
    return execController;
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, Action<? super Execution> continuation) throws Exception {
    ExecutionBacking backing = getBacking();
    backing.getInterceptors().add(execInterceptor);
    backing.intercept(ExecInterceptor.ExecType.COMPUTE, Collections.singletonList(execInterceptor), continuation);
  }

  @Override
  public ExecStarter exec() {
    return new ExecStarter() {
      private Action<? super Throwable> onError = LOG_UNCAUGHT;
      private Action<? super Execution> onComplete = noop();
      private Action<? super RegistrySpec> registry;

      @Override
      public ExecStarter onError(Action<? super Throwable> onError) {
        List<Throwable> seen = Lists.newLinkedList();
        this.onError = t -> {
          if (seen.size() < MAX_ERRORS_THRESHOLD) {
            seen.add(t);
            onError.execute(t);
          } else {
            seen.forEach(t::addSuppressed);
            LOGGER.error("Error handler " + onError + "reached maximum error threshold (might be caught in an error loop)", t);
          }
        };
        return this;
      }

      @Override
      public ExecStarter onComplete(Action<? super Execution> onComplete) {
        this.onComplete = onComplete;
        return this;
      }

      @Override
      public ExecStarter register(Action<? super RegistrySpec> registry) {
        this.registry = registry;
        return this;
      }

      @Override
      public void start(Action<? super Execution> action) {
        Action<? super Execution> effectiveAction = registry == null ? action : Action.join(registry, action);
        if (execController.isManagedThread() && threadBinding.get() == null) {
          new ExecutionBacking(execController, threadBinding, effectiveAction, onError, onComplete);
        } else {
          execController.getExecutor().submit(() -> new ExecutionBacking(execController, threadBinding, effectiveAction, onError, onComplete));
        }
      }
    };
  }

  @Override
  public <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return directPromise(SafeFulfiller.wrapping(action));
  }

  @Override
  public <T> Promise<T> blocking(final Callable<T> blockingOperation) {
    final ExecutionBacking backing = getBacking();
    final ExecController controller = backing.getController();
    return directPromise(f ->
        CompletableFuture.supplyAsync(() -> {
            List<Result<T>> holder = Lists.newArrayListWithCapacity(1);
            try {
              backing.intercept(ExecInterceptor.ExecType.BLOCKING, backing.getInterceptors(), execution ->
                  holder.add(0, Result.success(blockingOperation.call()))
              );
              return holder.get(0);
            } catch (Exception e) {
              return Result.<T>failure(e);
            }
          }, controller.getBlockingExecutor()
        ).thenAccept(f::accept)
    );
  }

  private <T> Promise<T> directPromise(Consumer<? super Fulfiller<? super T>> action) {
    return new DefaultPromise<>(this::getBacking, action);
  }

  @Override
  public <T> void stream(final Publisher<T> publisher, final Subscriber<? super T> subscriber) {
    final ExecutionBacking executionBacking = getBacking();

    directPromise((Consumer<Fulfiller<? super Subscription>>) fulfiller -> publisher.subscribe(new Subscriber<T>() {
      @Override
      public void onSubscribe(final Subscription subscription) {
        fulfiller.success(subscription);
      }

      @Override
      public void onNext(final T element) {
        executionBacking.streamExecution(execution -> subscriber.onNext(element));
      }

      @Override
      public void onComplete() {
        executionBacking.completeStreamExecution(execution -> subscriber.onComplete());
      }

      @Override
      public void onError(final Throwable cause) {
        executionBacking.completeStreamExecution(execution -> subscriber.onError(cause));
      }
    })).then(subscription -> executionBacking.streamExecution(execution -> subscriber.onSubscribe(subscription)));
  }

}
