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

import com.google.common.collect.ImmutableList;
import ratpack.api.Nullable;
import ratpack.exec.Promise;
import ratpack.func.internal.ConditionalAction;
import ratpack.util.Exceptions;

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
   * Executes the action against the given thing.
   *
   * @param t the thing to execute the action against
   * @throws Exception if anything goes wrong
   */
  void execute(T t) throws Exception;

  /**
   * Returns an action that does precisely nothing.
   *
   * @return an action that does precisely nothing
   */
  static <T> Action<T> noop() {
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
    return thing -> {
      for (Action<? super T> action : actions) {
        action.execute(thing);
      }
    };
  }

  /**
   * Returns a new action that executes this action and then the given action.
   *
   * @param action the action to execute after this action
   * @param <O> the type of object the action accepts
   * @return the newly created aggregate action
   */
  default <O extends T> Action<O> append(Action<? super O> action) {
    Action<T> self = this;
    return thing -> {
      self.execute(thing);
      action.execute(thing);
    };
  }

  /**
   * Returns a new action that executes the given action and then this action.
   *
   * @param action the action to execute before this action
   * @param <O> the type of object the action accepts
   * @return the newly created aggregate action
   */
  default <O extends T> Action<O> prepend(Action<? super O> action) {
    return action.append(this);
  }

  /**
   * Returns an action that receives a throwable and immediately throws it.
   *
   * @return an action that receives a throwable and immediately throws it
   */
  static Action<Throwable> throwException() {
    return throwable -> {
      throw Exceptions.toException(throwable);
    };
  }

  /**
   * An action that receives a throwable to thrown, suppressing the given value.
   *
   * @return an action that receives a throwable to thrown, suppressing the given value
   * @since 1.5
   */
  static Action<Throwable> suppressAndThrow(Throwable toSuppress) {
    return throwable -> {
      if (throwable != toSuppress) {
        throwable.addSuppressed(toSuppress);
      }
      throw Exceptions.toException(throwable);
    };
  }

  /**
   * Returns an action that immediately throws the given exception.
   * <p>
   * The exception is thrown via {@link ratpack.util.Exceptions#toException(Throwable)}
   *
   * @param <T> the argument type (anything, as the argument is ignored)
   * @param throwable the throwable to immediately throw when the returned action is executed
   * @return an action that immediately throws the given exception.
   */
  static <T> Action<T> throwException(final Throwable throwable) {
    return t -> {
      throw Exceptions.toException(throwable);
    };
  }

  static <T> Action<T> ignoreArg(final Block block) {
    return t -> block.execute();
  }

  /**
   * Executes the action with the given argument, then returns the argument.
   * <pre class="java">{@code
   * import ratpack.func.Action;
   * import java.util.ArrayList;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     assertEquals("foo", Action.with(new ArrayList<>(), list -> list.add("foo")).get(0));
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
    return action.with(t);
  }

  /**
   * Executes with the given argument, then returns the argument.
   * <pre class="java">{@code
   * import ratpack.func.Action;
   * import java.util.List;
   * import java.util.ArrayList;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     assertEquals("foo", run(list -> list.add("foo")).get(0));
   *   }
   *
   *   private static List<String> run(Action<? super List<String>> action) throws Exception {
   *     return action.with(new ArrayList<>());
   *   }
   * }
   * }</pre>
   * @param o the argument to execute the given action with
   * @param <O> the type of the argument
   * @return the given argument (i.e. {@code o})
   * @throws Exception any thrown by {@link #execute(Object)}
   */
  default <O extends T> O with(O o) throws Exception {
    execute(o);
    return o;
  }

  /**
   * Like {@link #with(Object, Action)}, but unchecks any exceptions thrown by the action via {@link ratpack.util.Exceptions#uncheck(Throwable)}.
   *
   * @param t the argument to execute the given action with
   * @param action the action to execute with the given argument
   * @param <T> the type of the argument
   * @return the given argument (i.e. {@code t})
   */
  static <T> T uncheckedWith(T t, Action<? super T> action) {
    return action.uncheckedWith(t);
  }

  /**
   * Like {@link #with(Object)}, but unchecks any exceptions thrown by the action via {@link ratpack.util.Exceptions#uncheck(Throwable)}.
   *
   * @param o the argument to execute  with
   * @param <O> the type of the argument
   * @return the given argument (i.e. {@code o})
   */
  default <O extends T> O uncheckedWith(O o) {
    return Exceptions.uncheck(() -> {
      execute(o);
      return o;
    });
  }

  /**
   * Creates a JDK {@link Consumer} from this action.
   * <p>
   * Any exceptions thrown by {@code this} action will be unchecked via {@link ratpack.util.Exceptions#uncheck(Throwable)} and rethrown.
   *
   * @return this function as a JDK style consumer.
   */
  default Consumer<T> toConsumer() {
    return t -> {
      try {
        execute(t);
      } catch (Exception e) {
        throw Exceptions.uncheck(e);
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

  /**
   * Creates a block that executes this action with the given value when called.
   *
   * @param value the value to execute this action with when the block is executed
   * @return a new block
   */
  default Block curry(T value) {
    return () -> execute(value);
  }

  /**
   * Creates an exception-taking action that executes the given action before throwing the exception.
   * <p>
   * This can be used with methods such as {@link Promise#onError(Action)} to simulate the Java {@code finally} construct.
   *
   * @param action the action to perform before throwing the exception
   * @return an action that performs the given action before throwing its argument
   * @since 1.5
   */
  static Action<Throwable> beforeThrow(Action<? super Throwable> action) {
    return t -> {
      try {
        action.execute(t);
      } catch (Exception e) {
        if (t != e) {
          e.addSuppressed(t);
        }
        throw e;
      }
      throw Exceptions.toException(t);
    };
  }

  /**
   * Creates an exception-taking action that executes the given block before throwing the exception.
   * <p>
   * This can be used with methods such as {@link Promise#onError(Action)} to simulate the Java {@code finally} construct.
   *
   * @param block the block to execute before throwing the exception
   * @return an action that executes the given block before throwing its argument
   * @since 1.5
   */
  static Action<Throwable> beforeThrow(Block block) {
    return beforeThrow(block.action());
  }

  /**
   * Creates an action that delegates to the given action if the given predicate applies, else delegates to {@link #noop()}.
   * <p>
   * This is equivalent to {@link #when(Predicate, Action, Action) when(predicate, action, noop())}.
   *
   * @param predicate the condition for the argument
   * @param action the action to execute if the predicate applies
   * @param <I> the type of argument
   * @return an action that delegates to the given action if the predicate applies, else noops
   * @see #when(Predicate, Action, Action)
   * @see #conditional(Action, Action)
   * @since 1.5
   */
  static <I> Action<I> when(Predicate<? super I> predicate, Action<? super I> action) {
    return when(predicate, action, Action.noop());
  }

  /**
   * Creates an action that delegates to the first action if the given predicate applies, else the second action.
   *
   * @param predicate the condition for the argument
   * @param onTrue the action to execute if the predicate applies
   * @param onFalse the action to execute if the predicate DOES NOT apply
   * @param <I> the type of argument
   * @return an action that delegates to the first action if the predicate applies, else the second argument
   * @see #when(Predicate, Action)
   * @see #conditional(Action, Action)
   * @since 1.5
   */
  static <I> Action<I> when(Predicate<? super I> predicate, Action<? super I> onTrue, Action<? super I> onFalse) {
    return Exceptions.uncheck(() -> conditional(onFalse, s -> s.when(predicate, onTrue)));
  }

  /**
   * Creates an action that delegates based on the specified conditions.
   * <p>
   * If no conditions match, an {@link IllegalArgumentException} will be thrown.
   * Use {@link #conditional(Action, Action)} alternatively to specify a different “else” strategy.
   *
   * @param conditions the conditions
   * @param <I> the input type
   * @return a conditional action
   * @see #conditional(Action, Action)
   * @throws Exception any thrown by {@code conditions}
   * @since 1.5
   */
  static <I> Action<I> conditional(Action<? super ConditionalSpec<I>> conditions) throws Exception {
    return conditional(i -> {
      throw new IllegalArgumentException("Unhandled argument: " + i);
    }, conditions);
  }

  /**
   * Creates an action that delegates based on the specified conditions.
   * <p>
   * If no condition applies, the {@code onElse} action will be delegated to.
   *
   * @param onElse the action to delegate to if no condition matches
   * @param conditions the conditions
   * @param <I> the input type
   * @return a conditional action
   * @see #conditional(Action)
   * @throws Exception any thrown by {@code conditions}
   * @since 1.5
   */
  static <I> Action<I> conditional(Action<? super I> onElse, Action<? super ConditionalSpec<I>> conditions) throws Exception {
    ImmutableList.Builder<ConditionalAction.Branch<I>> builder = ImmutableList.builder();
    conditions.execute(new ConditionalSpec<I>() {
      @Override
      public ConditionalSpec<I> when(Predicate<? super I> predicate, Action<? super I> action) {
        builder.add(new ConditionalAction.Branch<>(predicate, action));
        return this;
      }
    });

    return new ConditionalAction<>(builder.build(), onElse);
  }

  /**
   * A spec for adding conditions to a conditional action.
   *
   * @param <I> the input type
   * @see #conditional(Action, Action)
   * @since 1.5
   */
  interface ConditionalSpec<I> {
    ConditionalSpec<I> when(Predicate<? super I> predicate, Action<? super I> action);
  }
}
