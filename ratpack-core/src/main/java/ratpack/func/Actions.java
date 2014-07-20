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

/**
 * Factories for different {@link ratpack.func.Action} implementations.
 */
public abstract class Actions {

  private Actions() {

  }

  /**
   * Returns an action that does precisely nothing.
   *
   * @param <T> The type of parameter given to the action
   * @return an action that does precisely nothing
   */
  public static <T> Action<T> noop() {
    return new Action<T>() {
      @Override
      public void execute(T thing) throws Exception {
      }
    };
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

}
