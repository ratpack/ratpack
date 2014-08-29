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

  private final static Action<Throwable> THROW_EXCEPTION = new Action<Throwable>() {
    @Override
    public void execute(Throwable throwable) throws Exception {
      throw ExceptionUtils.toException(throwable);
    }
  };
  private static final Action<Object> NOOP = new Action<Object>() {
    @Override
    public void execute(Object thing) throws Exception {
    }
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
   * Returns an action that does precisely nothing.
   *
   * @return an action that does precisely nothing
   */
  public static Action<Throwable> throwException() {
    return THROW_EXCEPTION;
  }

  /**
   * Returns an action that acts on an action that acts on the given argument.
   * <p>
   * The returned action is effectively a callback for executing a callback for the given argument.
   *
   * @param t the argument to give to actions given to the returned action
   * @param <T> the type of the argument
   * @return an action that acts on an action that acts on the given argument
   */
  public static <T> Action<Action<? super T>> actionAction(final T t) {
    return new Action<Action<? super T>>() {
      @Override
      public void execute(Action<? super T> action) throws Exception {
        action.execute(t);
      }
    };
  }

}
