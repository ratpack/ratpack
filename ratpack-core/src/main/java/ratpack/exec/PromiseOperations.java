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

package ratpack.exec;

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.func.NoArgAction;
import ratpack.func.Predicate;

/**
 * Operations that can be performed on promises to define an asynchronous data flow.
 * <p>
 * These methods are available on {@link Promise} and {@link SuccessPromise}, but are defined on this separate interface for clarity.
 *
 * @param <T> the type of promised value
 */
public interface PromiseOperations<T> {

  // TODO define the semantics of user functions throwing

  /**
   * Transforms the promised value by applying the given function to it.
   * <pre class="java">{@code
   * import ratpack.exec.Execution;
   * import ratpack.exec.Promise;
   * import ratpack.func.Function;
   * import ratpack.test.UnitTest;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * import java.util.concurrent.Callable;
   *
   * public class Example {
   *
   *   public static void main(String[] args) throws Exception {
   *     try (ExecHarness harness = UnitTest.execHarness()) {
   *       ExecResult<String> result = harness.execute(new Function<Execution, Promise<String>>() {
   *         public Promise<String> apply(Execution execution) throws Exception {
   *           return execution.getControl()
   *             .blocking(new Callable<String>() {
   *               public String call() throws Exception {
   *                 return "foo";
   *               }
   *             })
   *             .map(new Function<String, String>() {
   *               public String apply(String s) throws Exception {
   *                 return s.toUpperCase();
   *               }
   *             })
   *             .map(new Function<String, String>() {
   *               public String apply(String s) throws Exception {
   *                 return s + "-BAR";
   *               }
   *             });
   *         }
   *       });
   *
   *       assert result.getValue().equals("FOO-BAR");
   *     }
   *   }
   *
   * }
   * }</pre>
   *
   * @param transformer the transformation to apply to the promised value
   * @param <O> the type of the transformed object
   * @return a promise for the transformed value
   */
  <O> Promise<O> map(Function<? super T, ? extends O> transformer);

  /**
   * Like {@link #map(Function)}, but performs the transformation on a blocking thread.
   * <p>
   * This is simply a more convenient form of using {@link ExecControl#blocking(java.util.concurrent.Callable)} and {@link #flatMap(Function)}.
   *
   * @param transformer the transformation to apply to the promised value, on a blocking thread
   * @param <O> the type of the transformed object
   * @return a promise for the transformed value
   */
  <O> Promise<O> blockingMap(Function<? super T, ? extends O> transformer);

  /**
   * Transforms the promised value by applying the given function to it that returns a promise for the transformed value.
   * <p>
   * This is useful when the transformation involves an asynchronous operation.
   * <pre class="java">{@code
   * import ratpack.exec.ExecControl;
   * import ratpack.exec.Execution;
   * import ratpack.exec.Promise;
   * import ratpack.func.Function;
   * import ratpack.test.UnitTest;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * import java.util.concurrent.Callable;
   *
   * public class Example {
   *
   *   public static void main(String[] args) throws Exception {
   *     try (ExecHarness harness = UnitTest.execHarness()) {
   *       ExecResult<String> result = harness.execute(new Function<Execution, Promise<String>>() {
   *         public Promise<String> apply(Execution execution) throws Exception {
   *           final ExecControl control = execution.getControl();
   *           return control
   *             .blocking(new Callable<String>() {
   *               public String call() throws Exception {
   *                 return "foo";
   *               }
   *             })
   *             .flatMap(new Function<String, Promise<String>>() {
   *               public Promise<String> apply(final String s) throws Exception {
   *                 return control.blocking(new Callable<String>() {
   *                   public String call() throws Exception {
   *                     return s.toUpperCase();
   *                   }
   *                 });
   *               }
   *             })
   *             .map(new Function<String, String>() {
   *               public String apply(String s) throws Exception {
   *                 return s + "-BAR";
   *               }
   *             });
   *         }
   *       });
   *
   *       assert result.getValue().equals("FOO-BAR");
   *     }
   *   }
   *
   * }
   * }</pre>
   * <p>
   * In the above example, {@code flatMap()} is being used because the transformation requires a blocking operation (it doesn't really in this case, but that's what the example is showing).
   * In this case, it would be more convenient to use {@link #blockingMap(Function)}.
   *
   * @param transformer the transformation to apply to the promised value
   * @param <O> the type of the transformed object
   * @see #blockingMap(Function)
   * @return a promise for the transformed value
   */
  <O> Promise<O> flatMap(Function<? super T, ? extends Promise<O>> transformer);

  /**
   * Handles (terminates) the promise if it matches the given predicate.
   * <p>
   * This can be useful to provide common validation or similar.
   * If the predicate applies to the promised value, it will not be propagated to the next subscriber.
   * <pre class="java">{@code
   * import ratpack.exec.ExecControl;
   * import ratpack.exec.Execution;
   * import ratpack.exec.Promise;
   * import ratpack.func.Function;
   * import ratpack.test.UnitTest;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.test.exec.ExecResult;
   *
   * import java.util.concurrent.Callable;
   *
   * public class Example {
   *
   *   public static void main(String[] args) throws Exception {
   *     try (ExecHarness harness = UnitTest.execHarness()) {
   *       ExecResult<String> result = harness.execute(new Function<Execution, Promise<String>>() {
   *         public Promise<String> apply(Execution execution) throws Exception {
   *           final ExecControl control = execution.getControl();
   *           return control
   *             .blocking(new Callable<String>() {
   *               public String call() throws Exception {
   *                 return "foo";
   *               }
   *             })
   *             .flatMap(new Function<String, Promise<String>>() {
   *               public Promise<String> apply(final String s) throws Exception {
   *                 return control.blocking(new Callable<String>() {
   *                   public String call() throws Exception {
   *                     return s.toUpperCase();
   *                   }
   *                 });
   *               }
   *             })
   *             .map(new Function<String, String>() {
   *               public String apply(String s) throws Exception {
   *                 return s + "-BAR";
   *               }
   *             });
   *         }
   *       });
   *
   *       assert result.getValue().equals("FOO-BAR");
   *     }
   *   }
   *
   * }
   * }</pre>
   * <p>
   * In the above example, {@code flatMap()} is being used because the transformation requires a blocking operation (it doesn't really in this case, but that's what the example is showing).
   * In this case, it would be more convenient to use {@link #blockingMap(Function)}.
   *
   * @param transformer the transformation to apply to the promised value
   * @param <O> the type of the transformed object
   * @see #blockingMap(Function)
   * @return a promise for the transformed value
   */
  Promise<T> route(Predicate<? super T> predicate, Action<? super T> action);

  Promise<T> onNull(NoArgAction action);
}
