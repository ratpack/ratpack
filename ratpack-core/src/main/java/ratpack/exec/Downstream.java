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

package ratpack.exec;

import ratpack.func.Action;

/**
 * A consumer of a single asynchronous value.
 * <p>
 * Downstreams {@link Upstream#connect(Downstream) connect} to {@link Upstream upstreams}.
 * Once connected, an upstream will invoke only one of either the {@link #success}, {@link #error} or {@link #complete} methods exactly once.
 *
 * @see Promise#transform(ratpack.func.Function)
 * @see ratpack.exec.Downstream.Decorator
 * @see ratpack.exec.Downstream.SameTypeDecorator
 * @param <T>
 */
public interface Downstream<T> {

  /**
   * Signals the successful production of the upstream value.
   *
   * @param value the upstream value
   */
  void success(T value);

  /**
   * Signals the unsuccessful production of the upstream value.
   *
   * @param throwable what went wrong
   */
  void error(Throwable throwable);

  /**
   * Signals that the upstream will not be providing a value, as it has terminated.
   */
  void complete();

  /**
   * Wrap this downstream, using the given action as the implementation of the {@link #success} method.
   * <p>
   * All {@link #error} and {@link #complete} signals will be forwarded to {@code this} downstream,
   * and the given action called with the value if {@link #success} is signalled.
   *
   * @param action the implementation of the success signal receiver for the returned downstream
   * @param <O> the type of item accepted
   * @return a downstream
   */
  default <O> Downstream<O> onSuccess(Action<? super O> action) {
    return new Decorator<O, T>(Downstream.this) {
      @Override
      public void success(O value) {
        try {
          action.execute(value);
        } catch (Throwable e) {
          Downstream.this.error(e);
        }
      }
    };
  }

  /**
   * Signals this downstream, based on the given result.
   *
   * @param result the result to signal
   */
  default void accept(ExecResult<? extends T> result) {
    if (result.isComplete()) {
      complete();
    } else if (result.isFailure()) {
      error(result.getThrowable());
    } else {
      success(result.getValue());
    }
  }

  /**
   * Signals this downstream, based on the given result.
   *
   * @param result the result to signal
   */
  default void accept(Result<? extends T> result) {
    if (result.isFailure()) {
      error(result.getThrowable());
    } else {
      success(result.getValue());
    }
  }

  /**
   * A base class for wrapping downstreams that delegates signals.
   *
   * @param <T> the type of object accepted by the decorated downstream
   * @param <O> the type of object accepted by {@code this} downstream
   */
  abstract class Decorator<T, O> implements Downstream<T> {

    /**
     * The decorated downstream.
     */
    protected final Downstream<? super O> delegate;

    /**
     * Constructor.
     *
     * @param delegate the decorated downstream.
     */
    public Decorator(Downstream<? super O> delegate) {
      this.delegate = delegate;
    }

    /**
     * Forwards the error to the decorated downstream.
     *
     * @param throwable what went wrong
     */
    @Override
    public void error(Throwable throwable) {
      delegate.error(throwable);
    }

    /**
     * Forwards the signal to the decorated downstream.
     */
    @Override
    public void complete() {
      delegate.complete();
    }
  }

  /**
   * A decorator base class that forwards the success signal.
   *
   * @param <T> the type of item accepted
   */
  class SameTypeDecorator<T> extends Decorator<T, T> {

    /**
     * Constructor.
     *
     * @param delegate the decorated downstream
     */
    public SameTypeDecorator(Downstream<? super T> delegate) {
      super(delegate);
    }

    /**
     * Forwards the value to the decorated downstream.
     *
     * @param value the upstream value
     */
    @Override
    public void success(T value) {
      delegate.success(value);
    }
  }
}
