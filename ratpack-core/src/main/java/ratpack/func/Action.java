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

import ratpack.api.Nullable;
import ratpack.util.ExceptionUtils;

import java.util.function.Consumer;

/**
 * A generic type for an object that does some work with a thing.
 * <p>
 * This type serves the same purpose as the JDK's {@link java.util.function.Consumer}, but allows throwing checked exceptions.
 * It contains methods for bridging to and from the JDK type.
 *
 * @param <T> The type of thing.
 */
@FunctionalInterface
public interface Action<T> {

  /**
   * Returns an action that does precisely nothing.
   *
   * @return an action that does precisely nothing
   */
  static Action<Object> noop() {
    return thing -> {
    };
  }

  /**
   * If the given action is {@code null}, returns {@link #noop()}, otherwise returns the given action.
   *
   * @param action an action, maybe {@code null}.
   * @param <T> the type of parameter received by the action
   * @return the given {@code action} param if it is not {@code null}, else a {@link #noop()}.
   */
  static <T> Action<? super T> noopIfNull(@Nullable Action<T> action) {
    if (action == null) {
      return noop();
    } else {
      return action;
    }
  }

  /**
   * Returns a new action that executes the given actions in order.
   *
   * @param actions the actions to join into one action
   * @param <T> the type of object the action accepts
   * @return the newly created aggregate action
   */
  @SafeVarargs
  static <T> Action<T> join(final Action<? super T>... actions) {
    return new Action<T>() {
      @Override
      public void execute(T thing) throws Exception {
        for (Action<? super T> action : actions) {
          action.execute(thing);
        }
      }
    };
  }

  /**
   * Returns an action that receives a throwable and immediately throws it.
   *
   * @return an action that receives a throwable and immediately throws it
   */
  static Action<Throwable> throwException() {
    return throwable -> {
      throw ExceptionUtils.toException(throwable);
    };
  }

  /**
   * Returns an action that immediately throws the given exception.
   * <p>
   * The exception is thrown via {@link ratpack.util.ExceptionUtils#toException(Throwable)}
   *
   * @param <T> the argument type (anything, as the argument is ignored)
   * @param throwable the throwable to immediately throw when the returned action is executed
   * @return an action that immediately throws the given exception.
   */
  static <T> Action<T> throwException(final Throwable throwable) {
    return t -> {
      throw ExceptionUtils.toException(throwable);
    };
  }

  static <T> Action<T> ignoreArg(final NoArgAction noArgAction) {
    return t -> noArgAction.execute();
  }

  /**
   * Executes the action with the given argument, then returns the argument.
   * <pre class="java">{@code
   * import ratpack.func.Action;
   * import java.util.ArrayList;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     assert Action.with(new ArrayList<>(), list -> list.add("foo")).get(0).equals("foo");
   *   }
   * }
   * }</pre>
   * @param t the argument to execute the given action with
   * @param action the action to execute with the given argument
   * @param <T> the type of the argument
   * @return the given argument (i.e. {@code t})
   * @throws Exception any thrown by {@code action}
   */
  static <T> T with(T t, Action<? super T> action) throws Exception {
    action.execute(t);
    return t;
  }

  /**
   * Like {@link #with(Object, Action)}, but unchecks any exceptions thrown by the action via {@link ExceptionUtils#uncheck(Throwable)}.
   *
   * @param t the argument to execute the given action with
   * @param action the action to execute with the given argument
   * @param <T> the type of the argument
   * @return the given argument (i.e. {@code t})
   */
  static <T> T uncheckedWith(T t, Action<? super T> action) {
    action.toConsumer().accept(t);
    return t;
  }

  /**
   * Executes the action against the given thing.
   *
   * @param t the thing to execute the action against
   * @throws Exception if anything goes wrong
   */
  void execute(T t) throws Exception;

  /**
   * Creates a JDK {@link Consumer} from this action.
   * <p>
   * Any exceptions thrown by {@code this} action will be unchecked via {@link ExceptionUtils#uncheck(Throwable)} and rethrown.
   *
   * @return this function as a JDK style consumer.
   */
  default Consumer<T> toConsumer() {
    return t -> {
      try {
        execute(t);
      } catch (Exception e) {
        throw ExceptionUtils.uncheck(e);
      }
    };
  }

  /**
   * Creates an action from a JDK consumer.
   *
   * @param consumer the JDK consumer
   * @param <T> the type of object this action accepts
   * @return the given consumer as an action
   */
  static <T> Action<T> from(Consumer<T> consumer) {
    return consumer::accept;
  }

}
