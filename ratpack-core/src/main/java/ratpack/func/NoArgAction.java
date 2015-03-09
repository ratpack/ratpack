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

import ratpack.util.Exceptions;

/**
 * An action that takes no arguments.
 * <p>
 * Used instead of {@link Runnable} to allow the throwing of checked exceptions.
 */
@FunctionalInterface
public interface NoArgAction {

  /**
   * Execute the action.
   *
   * @throws Exception any
   */
  public void execute() throws Exception;

  /**
   * Returns an action that immediately throws the given exception.
   * <p>
   * The exception is thrown via {@link ratpack.util.Exceptions#toException(Throwable)}
   *
   * @param throwable the throwable to immediately throw when the returned action is executed
   * @return an action that immediately throws the given exception.
   */
  static NoArgAction throwException(final Throwable throwable) {
    return () -> {
      throw Exceptions.toException(throwable);
    };
  }

}
