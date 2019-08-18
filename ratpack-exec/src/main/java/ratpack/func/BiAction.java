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

import ratpack.util.Exceptions;

import java.util.function.BiConsumer;

/**
 * A generic type for an object that does some work with 2 input things.
 * <p>
 * This type serves the same purpose as the JDK's {@link BiConsumer}, but allows throwing checked exceptions.
 * It contains methods for bridging to and from the JDK type.
 *
 * @param <T> the type of the first thing
 * @param <U> The type of the second thing
 */
@FunctionalInterface
public interface BiAction<T, U> {

  /**
   * Executes the action against the given thing.
   *
   * @param t the first thing to input to the action
   * @param u the second thing to input to the action
   * @throws Exception if anything goes wrong
   */
  void execute(T t, U u) throws Exception;

  /**
   * Returns a bi-action that does precisely nothing.
   *
   * @return a bi-action that does precisely nothing
   * @param <T> the type of the first thing
   * @param <U> The type of the second thing
   * @since 1.5
   */
  static <T, U> BiAction<T, U> noop() {
    return (a, b) -> {
    };
  }

  /**
   * Creates a JDK {@link BiConsumer} from this action.
   * <p>
   * Any exceptions thrown by {@code this} action will be unchecked via {@link Exceptions#uncheck(Throwable)} and rethrown.
   *
   * @return this function as a JDK style consumer.
   */
  default BiConsumer<T, U> toBiConsumer() {
    return (t, u) -> {
      try {
        execute(t, u);
      } catch (Exception e) {
        throw Exceptions.uncheck(e);
      }
    };
  }

  /**
   * Creates an bi-action from a JDK bi-consumer.
   *
   * @param consumer the JDK consumer
   * @param <T> the type of the first object this action accepts
   * @param <U> the type of the second object this action accepts
   * @return the given consumer as an action
   */
  static <T, U> BiAction<T, U> from(BiConsumer<T, U> consumer) {
    return consumer::accept;
  }
}
