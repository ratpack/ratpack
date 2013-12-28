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

package ratpack.groovy.internal;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.util.Action;

public abstract class ClosureUtil {

  private ClosureUtil() {
  }

  @SuppressWarnings("UnusedDeclaration") // used in GroovyChainDslFixture
  public static <D, R> R configureDelegateOnly(@DelegatesTo.Target D delegate, @DelegatesTo(strategy = Closure.DELEGATE_ONLY) Closure<R> configurer) {
    return configure(delegate, delegate, configurer, Closure.DELEGATE_ONLY);
  }

  public static <D, R> R configureDelegateFirst(@DelegatesTo.Target D delegate, @DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure<R> configurer) {
    return configure(delegate, delegate, configurer, Closure.DELEGATE_FIRST);
  }

  public static <D, A, R> R configureDelegateFirst(@DelegatesTo.Target D delegate, A argument, @DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure<R> configurer) {
    return configure(delegate, argument, configurer, Closure.DELEGATE_FIRST);
  }

  private static <R, D, A> R configure(D delegate, A argument, Closure<R> configurer, int resolveStrategy) {
    if (configurer == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Closure<R> clone = (Closure<R>) configurer.clone();
    clone.setDelegate(delegate);
    clone.setResolveStrategy(resolveStrategy);
    if (clone.getMaximumNumberOfParameters() == 0) {
      return clone.call();
    } else {
      return clone.call(argument);
    }
  }

  // Type token is here for in the future when @DelegatesTo supports this kind of API
  public static <T> Action<T> delegatingAction(@SuppressWarnings("UnusedParameters") Class<T> type, final Closure<?> configurer) {
    return new Action<T>() {
      public void execute(T object) {
        configureDelegateFirst(object, configurer);
      }
    };
  }

  public static Action<Object> delegatingAction(final Closure<?> configurer) {
    return delegatingAction(Object.class, configurer);
  }

  public static Action<Object> action(final Closure<?> closure) {
    final Closure<?> copy = closure.rehydrate(null, closure.getOwner(), closure.getThisObject());
    return new NoDelegateClosureAction(copy);
  }

  private static class NoDelegateClosureAction implements Action<Object> {

    private final Closure<?> copy;

    public NoDelegateClosureAction(Closure<?> copy) {
      this.copy = copy;
    }

    @Override
    public void execute(Object thing) {
      copy.call(thing);
    }
  }

  private static class DelegatingClosureRunnable<D, A> implements Runnable {
    private final D delegate;
    private final A argument;
    private final Closure<?> closure;

    private DelegatingClosureRunnable(D delegate, A argument, Closure<?> closure) {
      this.delegate = delegate;
      this.argument = argument;
      this.closure = closure;
    }

    @Override
    public void run() {
      configureDelegateFirst(delegate, argument, closure);
    }
  }

  public static <D, A> Runnable delegateFirstRunnable(D delegate, A argument, Closure<?> closure) {
    return new DelegatingClosureRunnable<>(delegate, argument, closure);
  }

  public static <T> Closure<T> returning(final T thing) {
    return new PassThroughClosure<>(thing);
  }

  public static Closure<Void> noop() {
    return returning((Void) null);
  }

  private static class PassThroughClosure<T> extends Closure<T> {

    static final long serialVersionUID = 0;

    private final T thing;

    public PassThroughClosure(T thing) {
      super(null);
      this.thing = thing;
    }

    @SuppressWarnings("UnusedDeclaration")
    protected T doCall() {
      return thing;
    }
  }
}
