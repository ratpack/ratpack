/*
 * Copyright 2013 the original author or authors.
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

import ratpack.util.ExceptionUtils;

import java.util.Objects;

/**
 * A single argument function.
 * <p>
 * This type serves the same purpose as the JDK's {@link java.util.function.Function}, but allows throwing checked exceptions.
 * It contains methods for bridging to and from the JDK type.
 *
 * @param <I> the type of the input
 * @param <O> the type of the output
 */
@FunctionalInterface
public interface Function<I, O> {

  /**
   * The function implementation.
   *
   * @param i the input to the function
   * @return the output of the function
   * @throws Exception any
   */
  O apply(I i) throws Exception;

  /**
   * Joins {@code this} function with the given function.
   *
   * <pre class="java">{@code
   * import ratpack.func.Function;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     Function<String, String> function = in -> in + "-bar";
   *     assert function.andThen(String::toUpperCase).apply("foo").equals("FOO-BAR");
   *   }
   * }
   * }</pre>
   * <p>
   * Analogous to {@link java.util.function.Function#andThen(java.util.function.Function)}.
   *
   * @param after the function to apply to the result of {@code this} function
   * @param <T> the type of the final output
   * @return the result of applying the given function to {@code this} function
   * @throws Exception any thrown by {@code this} or {@code after}
   */
  default <T> Function<I, T> andThen(Function<? super O, ? extends T> after) throws Exception {
    Objects.requireNonNull(after);
    return (I i) -> {
      O apply = apply(i);
      return after.apply(apply);
    };
  }

  /**
   * Joins the given function with {@code this} function.
   *
   * <pre class="java">{@code
   * import ratpack.func.Function;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     Function<String, String> function = String::toUpperCase;
   *     assert function.compose(in -> in + "-BAR").apply("foo").equals("FOO-BAR");
   *   }
   * }
   * }</pre>
   * <p>
   * Analogous to {@link java.util.function.Function#compose(java.util.function.Function)}.
   *
   * @param before the function to apply {@code this} function to the result of
   * @param <T> the type of the new input
   * @return the result of applying {@code this} function to the result of the given function
   * @throws Exception any thrown by {@code this} or {@code before}
   */
  default <T> Function<T, O> compose(Function<? super T, ? extends I> before) throws Exception {
    Objects.requireNonNull(before);
    return (T t) -> apply(before.apply(t));
  }

  /**
   * Converts {@code this} function into the equivalent JDK type.
   * <p>
   * Any exceptions thrown by {@code this} function will be unchecked via {@link ExceptionUtils#uncheck(Throwable)} and rethrown.
   *
   * @return this function as a JDK style function.
   */
  default java.util.function.Function<I, O> toFunction() {
    return (t) -> {
      try {
        return apply(t);
      } catch (Exception e) {
        throw ExceptionUtils.uncheck(e);
      }
    };
  }

  /**
   * Converts {@code this} function into the equivalent Guava type.
   * <p>
   * Any exceptions thrown by {@code this} function will be unchecked via {@link ExceptionUtils#uncheck(Throwable)} and rethrown.
   *
   * @return this function as a Guava style function.
   */
  default com.google.common.base.Function<I, O> toGuavaFunction() {
    return (t) -> {
      try {
        return apply(t);
      } catch (Exception e) {
        throw ExceptionUtils.uncheck(e);
      }
    };
  }

  /**
   * Creates a function of this type from a JDK style function.
   *
   * @param function a JDK style function
   * @param <I> the input type
   * @param <O> the output type
   * @return a Ratpack style function wrapping the given JDK function
   */
  static <I, O> Function<I, O> from(java.util.function.Function<I, O> function) {
    Objects.requireNonNull(function);
    return function::apply;
  }

  /**
   * Creates a function of this type from a Guava style function.
   *
   * @param function a Guava style function
   * @param <I> the input type
   * @param <O> the output type
   * @return a Ratpack style function wrapping the given Guava function
   */
  static <I, O> Function<I, O> fromGuava(com.google.common.base.Function<I, O> function) {
    Objects.requireNonNull(function);
    return function::apply;
  }

  /**
   * Returns an identity function (return value always same as input).
   *
   * @param <T> the type of the input and output objects to the function
   * @return a function that always returns its input argument
   */
  static <T> Function<T, T> identity() {
    return t -> t;
  }
}
