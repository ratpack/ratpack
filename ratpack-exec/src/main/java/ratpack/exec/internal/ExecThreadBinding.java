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

import io.netty.util.concurrent.FastThreadLocal;
import ratpack.api.Nullable;
import ratpack.exec.ExecController;
import ratpack.exec.ExecutionException;
import ratpack.exec.UnmanagedThreadException;
import ratpack.func.Factory;

import java.util.Optional;

public class ExecThreadBinding {

  private final Thread thread;
  private final boolean compute;
  private final ExecController execController;
  private DefaultExecution execution;

  public ExecThreadBinding(Thread thread, boolean compute, ExecController execController) {
    this.thread = thread;
    this.compute = compute;
    this.execController = execController;
  }

  private static final FastThreadLocal<ExecThreadBinding> STORAGE = new FastThreadLocal<>();

  public static void bind(boolean compute, ExecController execController) {
    STORAGE.set(new ExecThreadBinding(Thread.currentThread(), compute, execController));
  }

  public static void unbind() {
    STORAGE.remove();
  }

  public static <T> T bindFor(boolean compute, ExecController execController, Factory<T> function) throws Exception {
    ExecThreadBinding current = STORAGE.get();
    if (current != null && current.getExecController() == execController) {
      return function.create();
    }

    bind(compute, execController);
    try {
      return function.create();
    } finally {
      unbind();
      if (current != null) {
        STORAGE.set(current);
      }
    }
  }

  public boolean isCurrentThread() {
    return thread == Thread.currentThread();
  }

  @Nullable
  public static ExecThreadBinding get() {
    return STORAGE.get();
  }

  public static Optional<ExecThreadBinding> maybeGet() {
    return Optional.ofNullable(STORAGE.get());
  }

  public static ExecThreadBinding require() {
    ExecThreadBinding execThreadBinding = STORAGE.get();
    if (execThreadBinding == null) {
      throw new UnmanagedThreadException();
    } else {
      return execThreadBinding;
    }
  }

  public DefaultExecution getExecution() {
    return execution;
  }

  public void setExecution(DefaultExecution execution) {
    this.execution = execution;
  }

  public boolean isCompute() {
    return compute;
  }

  public ExecController getExecController() {
    return execController;
  }

  public static void requireComputeThread(String message) {
    if (!require().isCompute()) {
      throw new ExecutionException(toMessage(message));
    }
  }

  public static void requireBlockingThread(String message) {
    if (require().isCompute()) {
      throw new ExecutionException(toMessage(message));
    }
  }

  private static String toMessage(String message) {
    return message + " - current thread name = '" + Thread.currentThread().getName() + "'.";
  }

}
