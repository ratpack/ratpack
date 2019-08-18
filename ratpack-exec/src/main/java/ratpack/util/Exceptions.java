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

package ratpack.util;

import ratpack.api.UncheckedException;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.Block;
import ratpack.func.Factory;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Utility methods for dealing with exceptions.
 */
public abstract class Exceptions {

  /**
   * Converts the given throwable to a {@link RuntimeException} if necessary.
   * <p>
   * If {@code throwable} is an {@link Error}, it will be thrown.
   * <p>
   * If {@code throwable} is a {@link RuntimeException}, it will be returned unchanged.
   * <p>
   * If {@code throwable} is not a {@link RuntimeException}, a newly created {@link UncheckedException} will be returned with the original throwable as the cause and with no message.
   *
   * @param throwable the throwable to ensure is a runtime exception
   * @return a runtime throwable
   */
  public static RuntimeException uncheck(Throwable throwable) {
    if (throwable instanceof Error) {
      throw (Error) throwable;
    }
    if (throwable instanceof RuntimeException) {
      return (RuntimeException) throwable;
    } else if (throwable instanceof IOException) {
      throw new UncheckedIOException((IOException) throwable);
    } else {
      return new UncheckedException(throwable);
    }
  }

  /**
   * Converts the given throwable to an {@link Exception} if necessary.
   * <p>
   * If {@code throwable} is an {@link Error}, it will be thrown.
   * <p>
   * If {@code throwable} is an {@link Exception}, it will be returned unchanged.
   * <p>
   * If {@code throwable} is not an {@link Exception}, a newly created {@link UncheckedException} will be returned with the original throwable as the cause and with no message.
   *
   * @param throwable the throwable to ensure is an exception
   * @return a runtime throwable
   */
  public static Exception toException(Throwable throwable) {
    if (throwable instanceof Error) {
      throw (Error) throwable;
    }
    if (throwable instanceof Exception) {
      return (Exception) throwable;
    } else {
      return new UncheckedException(throwable);
    }
  }

  /**
   * Throws the given throwable if it is an {@link Error}, otherwise does nothing.
   *
   * @param throwable the throwable to throw if it is an {@link Error}
   */
  public static void throwIfError(Throwable throwable) {
    if (throwable instanceof Error) {
      throw (Error) throwable;
    }
  }

  /**
   * Executes the given factory, returning its result and unchecking any exceptions it throws.
   * <p>
   * If the factory throws an exception, it will be thrown via {@link #uncheck(Throwable)}.
   *
   * @param factory a value producer
   * @param <T> the type of value produced
   * @return the value produced by the given factory
   */
  public static <T> T uncheck(Factory<T> factory) {
    try {
      return factory.create();
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static void uncheck(Block action) {
    try {
      action.execute();
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Executes the given action with the provided input argument, unchecking any exceptions it throws.
   * <p>
   * If the factory throws an exception, it will be thrown via {@link #uncheck(Throwable)}.
   *
   * @param input the input argument to the action
   * @param action the work to perform against the input
   * @param <T> the type of the input argument
   */
  public static <T> void uncheck(T input, Action<T> action) {
    try {
      action.execute(input);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Executes the given action with the provided input arguments, unchecking any exceptions it throws.
   * <p>
   * If the factory throws an exception, it will be thrown via {@link #uncheck(Throwable)}.
   *
   * @param input1 the first input argument to the action
   * @param input2 the second input argument to the action
   * @param action the work to perform against the input
   * @param <T> the type of the first input argument
   * @param <U> the type of the second input argument
   */
  public static <T, U> void uncheck(T input1, U input2, BiAction<T, U> action) {
    try {
      action.execute(input1, input2);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

}
