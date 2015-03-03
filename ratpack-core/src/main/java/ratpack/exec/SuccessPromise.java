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

import ratpack.api.NonBlocking;
import ratpack.func.Action;

/**
 * A promise of a successful outcome.
 * <p>
 * See {@link Promise}.
 *
 * @param <T> the type of the outcome object
 * @see Promise
 */
public interface SuccessPromise<T> extends PromiseOperations<T> {

  /**
   * Specifies what should be done with the promised object when it becomes available.
   *
   * @param then the receiver of the promised value
   */
  @NonBlocking
  void then(Action<? super T> then);

  /**
   * Returns this success promise as a promise.
   * <p>
   * This method can be used to create a promise, when you have a success promise but need a promise.
   * This can happen, for example, when using {@link #apply(ratpack.func.Function)}.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.List;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   private static final List<String> LOG = new LinkedList<>();
   *
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(e ->
   *         e.blocking(() -> { throw new Exception("bang!"); })
   *           .apply(Example::logErrors)
   *           .onError(t -> LOG.add("in onError: " + t.getMessage()))
   *           .then(t -> LOG.add("in then: " + t))
   *     );
   *
   *     assertEquals(Arrays.asList("in logErrors: bang!"), LOG);
   *   }
   *
   *   public static <T> Promise<T> logErrors(Promise<T> input) throws Exception {
   *     return input.onError(t -> LOG.add("in logErrors: " + t.getMessage())).toPromise();
   *   }
   * }
   * }</pre>
   * <p>
   * The error handling strategy defined as part of {@code this} success promise, supersedes any defined on the returned promise of this method.
   *
   * @return a newly created promise, from {@code this} success promise
   */
  Promise<T> toPromise();

}
