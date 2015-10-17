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
import ratpack.exec.ExecController;
import ratpack.exec.ExecutionException;
import ratpack.exec.UnmanagedThreadException;

import java.util.Optional;

public class ThreadBinding {

  private final boolean compute;
  private final ExecController execController;

  public ThreadBinding(boolean compute, ExecController execController) {
    this.compute = compute;
    this.execController = execController;
  }

  private static final FastThreadLocal<ThreadBinding> STORAGE = new FastThreadLocal<>();

  static void bind(boolean compute, ExecController execController) {
    STORAGE.set(new ThreadBinding(compute, execController));
  }

  public static Optional<ThreadBinding> get() {
    return Optional.ofNullable(STORAGE.get());
  }

  public boolean isCompute() {
    return compute;
  }

  public ExecController getExecController() {
    return execController;
  }

  public static void requireComputeThread(String message) {
    if (!get().orElseThrow(UnmanagedThreadException::new).isCompute()) {
      throw new ExecutionException(toMessage(message));
    }
  }

  public static void requireBlockingThread(String message) {
    if (get().orElseThrow(UnmanagedThreadException::new).isCompute()) {
      throw new ExecutionException(toMessage(message));
    }
  }

  private static String toMessage(String message) {
    return message + " - current thread name = '" + Thread.currentThread().getName() + "'.";
  }

}
