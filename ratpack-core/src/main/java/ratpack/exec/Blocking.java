/*
 * Copyright 2015 the original author or authors.
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

package ratpack.exec;

import ratpack.exec.internal.DefaultPromise;
import ratpack.exec.internal.ExecutionBacking;
import ratpack.func.Factory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Facilitates working with code that blocks (e.g. synchronous IO)
 */
public abstract class Blocking {

  private Blocking() {
  }

  public static <T> Promise<T> get(Factory<T> factory) {
    return new DefaultPromise<>(downstream -> {
      ExecutionBacking backing = ExecutionBacking.require();
      backing.streamSubscribe(streamHandle ->
          CompletableFuture.supplyAsync(
            new Supplier<Result<T>>() {
              Result<T> result;

              @Override
              public Result<T> get() {
                try {
                  ExecutionBacking.THREAD_BINDING.set(backing);
                  backing.intercept(ExecInterceptor.ExecType.BLOCKING, backing.getAllInterceptors().iterator(), () -> {
                    T value = factory.create();
                    result = Result.success(value);
                  });
                  return result;
                } catch (Exception e) {
                  return Result.<T>error(e);
                } finally {
                  ExecutionBacking.THREAD_BINDING.remove();
                }
              }
            }, backing.getExecution().getController().getBlockingExecutor()
          ).thenAcceptAsync(v ->
              streamHandle.complete(() ->
                  downstream.accept(v)
              ),
            backing.getEventLoop()
          )
      );
    });

  }

  // nb. would replace promise.block();
  public static <T> T on(Promise<T> promise) throws Exception {
    return promise.block();
  }

}
