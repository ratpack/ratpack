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

package ratpack.func;

import ratpack.api.Nullable;
import ratpack.util.ExceptionUtils;

/**
 * Factories for different {@link ratpack.func.Action} implementations.
 */
public abstract class Actions {

  private final static Action<Throwable> THROW_EXCEPTION = throwable -> {
    throw ExceptionUtils.toException(throwable);
  };

  private static final Action<Object> NOOP = thing -> {
  };

  private Actions() {

  }

  /**
   * Returns an action that does precisely nothing.
   *
   * @return an action that does precisely nothing
   */
  public static Action<Object> noop() {
    return NOOP;
  }

  /**
   * If the given action is {@code null}, returns {@link #noop()}, otherwise returns the given action.
   *
   * @param action an action, maybe {@code null}.
   * @param <T> the type of parameter received by the action
   * @return the given {@code action} param if it is not {@code null}, else a {@link #noop()}.
   */
  public static <T> Action<? super T> noopIfNull(@Nullable Action<T> action) {
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
  public static <T> Action<T> join(final Action<? super T>... actions) {
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
  public static Action<Throwable> throwException() {
    return THROW_EXCEPTION;
  }

  /**
   * Returns an action that immediately throws the given exception.
   * <p>
   * The exception is thrown via {@link ExceptionUtils#toException(Throwable)}
   *
   * @param <T> the argument type (anything, as the argument is ignored)
   * @param throwable the throwable to immediately throw when the returned action is executed
   * @return an action that immediately throws the given exception.
   */
  public static <T> Action<T> throwException(final Throwable throwable) {
    return t -> {
      throw ExceptionUtils.toException(throwable);
    };
  }

  public static <T> Action<T> ignoreArg(final NoArgAction noArgAction) {
    return t -> noArgAction.execute();
  }

  /**
   * Executes the action with the given argument, then returns the argument.
   * <pre class="java">{@code
   * import ratpack.func.Actions;
   * import java.util.ArrayList;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     assert Actions.with(new ArrayList<>(), list -> list.add("foo")).get(0).equals("foo");
   *   }
   * }
   * }</pre>
   * @param t the argument to execute the given action with
   * @param action the action to execute with the given argument
   * @param <T> the type of the argument
   * @return the given argument (i.e. {@code t})
   * @throws Exception any thrown by {@code action}
   */
  public static <T> T with(T t, Action<? super T> action) throws Exception {
    action.execute(t);
    return t;
  }

}
