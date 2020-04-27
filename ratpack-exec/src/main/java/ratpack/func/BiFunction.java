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

package ratpack.func;

/**
 * A two argument function.
 * <p>
 * This type serves the same purpose as the JDK's {@link java.util.function.BiFunction}, but allows throwing checked exceptions.
 *
 * @param <I1> the type of the first input
 * @param <I2> the type of the second input
 * @param <O> the type of the output
 */
@FunctionalInterface
public interface BiFunction<I1, I2, O> {

  /**
   * The function implementation.
   *
   * @param i1 the first input to the function
   * @param i1 the second input to the function
   * @return the output of the function
   * @throws Exception any
   */
  O apply(I1 i1, I2 i2) throws Exception;

  /**
   * Binds a value to the first input of the function.
   *
   * @param i1 the first input to the function
   * @return a new function that accepts the remaining arguments.
   */
  default Function<I2, O> curry(I1 i1) {
    return i2 -> apply(i1, i2);
  }

  /**
   * Joins {@code this} function with the given function.
   *
   * <pre class="java">{@code
   * import ratpack.func.BiFunction;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     BiFunction<String, String, String> function = (in1, in2) -> in1 + "-" + in2 + "-bar";
   *     assertEquals("FOO-BAZ-BAR", function.andThen(String::toUpperCase).apply("foo", "baz"));
   *   }
   * }
   * }</pre>
   * <p>
   * Analogous to {@link java.util.function.BiFunction#andThen(java.util.function.Function)}.
   *
   * @param transform the function to apply to the result of {@code this} function
   * @param <V> the type of the final output
   * @return the result of applying the given function to {@code this} function
   */
  default <V> BiFunction<I1, I2, V> andThen(Function<? super O, ? extends V> transform) {
    return (i1, i2) -> transform.apply(apply(i1, i2));
  }

}
