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

package ratpack.rx;

import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.rx.internal.DefaultSchedulers;
import ratpack.rx.internal.ExecControllerBackedScheduler;
import ratpack.util.ExceptionUtils;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action2;
import rx.plugins.RxJavaObservableExecutionHook;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static ratpack.util.ExceptionUtils.toException;

/**
 * Provides integration with <a href="https://github.com/Netflix/RxJava">RxJava</a>.
 * <p>
 * <b>IMPORTANT:</b> the {@link #initialize()} method must be called to fully enable integration.
 * <p>
 * The methods of this class provide bi-directional conversion between Ratpack's {@link Promise} and RxJava's {@link Observable}.
 * This allows Ratpack promise based API to be integrated into an RxJava based app and vice versa.
 * <p>
 * When using observables for asynchronous actions, it is generally required to wrap promises created by an {@link ExecControl} in order to integrate with Ratpack's execution model.
 * This typically means using {@link ExecControl#promise(Action)} or {@link ExecControl#blocking(java.util.concurrent.Callable)} to initiate the operation and then wrapping with {@link #observe(Promise)} or similar.
 * <p>
 * To test observable based services that use Ratpack's execution semantics, use the {@code ExecHarness} and convert the observable back to a promise with {@link #promise(Observable)}.
 * <p>
 * The methods in this class are also provided as <a href="http://docs.groovy-lang.org/latest/html/documentation/#_extension_modules">Groovy Extensions</a>.
 * When using Groovy, each static method in this class is able to act as an instance-level method against the {@link Observable} type.
 * </p>
 */
public abstract class RxRatpack {

  /**
   * Registers an {@link RxJavaObservableExecutionHook} with RxJava that provides a default error handling strategy of forwarding exceptions to the execution error handler.
   * <p>
   * This method is idempotent.
   * It only needs to be called once per JVM, regardless of how many Ratpack applications are running within the JVM.
   * <p>
   * For a Java application, a convenient place to call this is in the handler factory implementation.
   * <pre class="java">{@code
   * import ratpack.error.ServerErrorHandler;
   * import ratpack.rx.RxRatpack;
   * import ratpack.test.handling.RequestFixture;
   * import ratpack.test.handling.HandlingResult;
   * import rx.Observable;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     RxRatpack.initialize(); // must be called once per JVM
   *
   *     HandlingResult result = RequestFixture.requestFixture().handleChain(chain -> {
   *       chain.register(registry ->
   *         registry.add(ServerErrorHandler.class, (context, throwable) ->
   *           context.render("caught by error handler: " + throwable.getMessage())
   *         )
   *       );
   *
   *       chain.get(ctx -> Observable.<String>error(new Exception("!")).subscribe((s) -> {}));
   *     });
   *
   *     assertEquals("caught by error handler: !", result.rendered(String.class));
   *   }
   * }
   * }</pre>
   */
  @SuppressWarnings("deprecation")
  public static void initialize() {
    RxJavaPlugins plugins = RxJavaPlugins.getInstance();

    ExecutionHook ourHook = new ExecutionHook();
    try {
      plugins.registerObservableExecutionHook(ourHook);
    } catch (IllegalStateException e) {
      RxJavaObservableExecutionHook existingHook = plugins.getObservableExecutionHook();
      if (!(existingHook instanceof ExecutionHook)) {
        throw new IllegalStateException("Cannot install RxJava integration because another execution hook (" + existingHook.getClass() + ") is already installed");
      }
    }

    System.setProperty("rxjava.plugin." + RxJavaSchedulersHook.class.getSimpleName() + ".implementation", DefaultSchedulers.class.getName());
    rx.plugins.RxJavaSchedulersHook existingSchedulers = plugins.getSchedulersHook();
    if (!(existingSchedulers instanceof DefaultSchedulers)) {
      throw new IllegalStateException("Cannot install RxJava integration because another set of default schedulers (" + existingSchedulers.getClass() + ") is already installed");
    }
  }

  /**
   * A scheduler that uses the application event loop and initialises each job as an {@link ratpack.exec.Execution} (via {@link ratpack.exec.ExecControl#exec()}).
   *
   * @param execController the execution controller to back the scheduler
   * @return a scheduler
   */
  public static Scheduler scheduler(final ExecController execController) {
    return new ExecControllerBackedScheduler(execController);
  }

  /**
   * Binds the given observable to the current execution, allowing integration of third-party asynchronous observables with Ratpack's execution model.
   * <p>
   * This method is useful when you want to consume an asynchronous observable within a Ratpack execution, as an observable.
   * It is just a combination of {@link #promise(Observable)} and {@link #observeEach(Promise)}.
   * <p>
   * <pre class="java">{@code
   * import rx.Observable;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.rx.RxRatpack;
   * import java.util.Arrays;
   * import java.util.List;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     Observable<String> asyncObservable = Observable.create(subscriber ->
   *       new Thread(() -> {
   *         subscriber.onNext("foo");
   *         subscriber.onNext("bar");
   *         subscriber.onCompleted();
   *       }).start()
   *     );
   *
   *     List<String> strings = ExecHarness.yieldSingle(e ->
   *       RxRatpack.promise(asyncObservable.compose(RxRatpack::bindExec))
   *     ).getValue();
   *
   *     assertEquals(Arrays.asList("foo", "bar"), strings);
   *   }
   * }
   * }</pre>
   * <p>
   *
   * @param source the observable source
   * @param <T> the type of item observed
   * @return an observable stream equivalent to the given source
   * @see #observeEach(Promise)
   * @see #promise(Observable)
   */
  public static <T> Observable<T> bindExec(Observable<T> source) {
    return ExceptionUtils.uncheck(() -> promise(source).to(RxRatpack::observeEach));
  }

  /**
   * Converts a Ratpack promise into an Rx observable.
   * <p>
   * For example, this can be used to observe blocking operations.
   * <pre class="java">{@code
   * import ratpack.rx.RxRatpack;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static String value;
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(e ->
   *       e.promiseOf("hello world")
   *         .to(RxRatpack::observe)
   *         .map(String::toUpperCase)
   *         .subscribe(s -> value = s)
   *     );
   *
   *     assertEquals("HELLO WORLD", value);
   *   }
   * }
   * }</pre>
   *
   * @param promise the promise
   * @param <T> the type of value promised
   * @return an observable for the promised value
   */
  public static <T> Observable<T> observe(Promise<T> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, (thing, subscriber) -> subscriber.onNext(thing)));
  }

  /**
   * Converts a Rx {@link Observable} into a Ratpack {@link Promise}.
   * <p>
   * For example, this can be used unit test Rx observables.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import rx.Observable;
   * import ratpack.rx.RxRatpack;
   *
   * import java.util.List;
   *
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   static class AsyncService {
   *     // Our method under test
   *     // Typically this would be returning an Observable of an asynchronously produced value (using RxRatpack.observe()),
   *     // but for this example we are just faking it with a simple literal observable
   *     public <T> Observable<T> observe(final T value) {
   *       return Observable.just(value);
   *     }
   *   }
   *
   *   public static void main(String[] args) throws Throwable {
   *
   *     // set up the code under test that returns observables
   *     final AsyncService service = new AsyncService();
   *
   *     // exercise the async code using the harness, blocking until the promised value is available
   *     ExecResult<List<String>> result = ExecHarness.yieldSingle(execution -> RxRatpack.promise(service.observe("foo")));
   *
   *     List<String> results = result.getValue();
   *     assertEquals(1, results.size());
   *     assertEquals("foo", results.get(0));
   *   }
   * }
   * }</pre>
   *
   * @param observable the observable
   * @param <T> the type of the value observed
   * @return a promise that returns all values from the observable
   * @see #promiseSingle(Observable)
   */
  public static <T> Promise<List<T>> promise(Observable<T> observable) {
    return ExecControl.current().promise(f -> observable.toList().subscribe(f::success, f::error));
  }

  /**
   * Convenience for converting an {@link Observable} to a {@link Promise} when it is known that the observable sequence is of zero or one elements.
   * <p>
   * Has the same behavior as {@link #promise(Observable)}, except that the list representation of the sequence is “unpacked”.
   * <p>
   * If the observable sequence produces no elements, the promised value will be {@code null}.
   * If the observable sequence produces one element, the promised value will be that element.
   * If the observable sequence produces more than one element, the promised will fail with an {@link IllegalAccessException}.
   *
   * @param observable the observable
   * @param <T> the type of the value observed
   * @return a promise that returns the sole value from the observable
   * @see #promise(Observable)
   */
  public static <T> Promise<T> promiseSingle(Observable<T> observable) {
    return ExecControl.current().promise(f -> observable.single().subscribe(f::success, f::error));
  }

  /**
   * Converts a Ratpack promise of an iterable value into an Rx observable for each element of the promised iterable.
   * <p>
   * The promised iterable will be emitted to the observer one element at a time.
   * For example, this can be used to observe background operations that produce some kind of iterable&hellip;
   *
   * <pre class="java">{@code
   * import ratpack.rx.RxRatpack;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.List;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   private static final List<String> LOG = new LinkedList<>();
   *
   *   public static void main(String... args) throws Exception {
   *     ExecHarness.runSingle(e ->
   *         e.blocking(() -> Arrays.asList("foo", "bar"))
   *           .to(RxRatpack::observeEach)
   *           .subscribe(LOG::add)
   *     );
   *
   *     assertEquals(Arrays.asList("foo", "bar"), LOG);
   *   }
   * }
   * }</pre>
   *
   * @param promise the promise
   * @param <T> the element type of the promised iterable
   * @param <I> the type of iterable
   * @return an observable for each element of the promised iterable
   * @see #observe(ratpack.exec.Promise)
   */
  public static <T, I extends Iterable<T>> Observable<T> observeEach(Promise<I> promise) {
    return Observable.create(new PromiseSubscribe<>(promise, (things, subscriber) -> {
      for (T thing : things) {
        subscriber.onNext(thing);
      }
    }));
  }

  /**
   * Parallelize an observable by creating a new Ratpack execution for each element.
   * <p>
   * <pre class="java">{@code
   * import ratpack.rx.RxRatpack;
   * import ratpack.util.ExceptionUtils;
   * import ratpack.test.exec.ExecHarness;
   *
   * import rx.Observable;
   *
   * import java.util.List;
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.Collection;
   * import java.util.Collections;
   * import java.util.concurrent.CyclicBarrier;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     RxRatpack.initialize();
   *
   *     CyclicBarrier barrier = new CyclicBarrier(5);
   *
   *     try (ExecHarness execHarness = ExecHarness.harness(6)) {
   *       List<Integer> values = execHarness.yield(execution ->
   *         RxRatpack.promise(
   *           Observable.just(1, 2, 3, 4, 5)
   *             .compose(RxRatpack::forkEach) // parallelize
   *             .doOnNext(value -> ExceptionUtils.uncheck(() -> barrier.await())) // wait for all values
   *             .map(integer -> integer.intValue() * 2)
   *             .serialize()
   *         )
   *       ).getValue();
   *
   *       List<Integer> sortedValues = new LinkedList<>(values);
   *       Collections.sort(sortedValues);
   *       assertEquals(Arrays.asList(2, 4, 6, 8, 10), sortedValues);
   *     }
   *   }
   * }
   * }</pre>
   *
   * @param observable the observable sequence to process each element of in a forked execution
   * @param <T> the element type
   * @return an observable
   */
  public static <T> Observable<T> forkEach(Observable<T> observable) {
    ExecControl current = ExecControl.current();

    return observable.<T>lift(downstream -> new Subscriber<T>(downstream) {

      private final AtomicInteger wip = new AtomicInteger(1);
      private final AtomicBoolean closed = new AtomicBoolean();

      @Override
      public void onCompleted() {
        maybeDone();
      }

      @Override
      public void onError(final Throwable e) {
        terminate(() -> downstream.onError(e));
      }

      private void maybeDone() {
        if (wip.decrementAndGet() == 0) {
          terminate(downstream::onCompleted);
        }
      }

      private void terminate(Runnable runnable) {
        if (closed.compareAndSet(false, true)) {
          runnable.run();
        }
      }

      @Override
      public void onNext(final T t) {
        // Avoid the overhead of creating executions if downstream is no longer interested
        if (isUnsubscribed() || closed.get()) {
          return;
        }

        wip.incrementAndGet();
        current.exec()
          .onComplete(e -> this.maybeDone())
          .onError(this::onError)
          .start(e -> {
            if (!closed.get()) {
              downstream.onNext(t);
            }
          });
      }
    });
  }

  private static class PromiseSubscribe<T, S> implements Observable.OnSubscribe<S> {

    private final Promise<T> promise;
    private final Action2<T, Subscriber<? super S>> emitter;

    public PromiseSubscribe(Promise<T> promise, Action2<T, Subscriber<? super S>> emitter) {
      this.promise = promise;
      this.emitter = emitter;
    }

    @Override
    public void call(final Subscriber<? super S> subscriber) {
      try {
        promise
          .onError(throwable -> {
            try {
              subscriber.onError(throwable);
            } catch (OnErrorNotImplementedException e) {
              throw toException(e.getCause());
            }
          })
          .then(thing -> {
            try {
              emitter.call(thing, subscriber);
              subscriber.onCompleted();
            } catch (OnErrorNotImplementedException e) {
              throw toException(e.getCause());
            }
          });
      } catch (Exception e) {
        throw ExceptionUtils.uncheck(e);
      }
    }

  }

  private static class ExecutionHook extends RxJavaObservableExecutionHook {

    @Override
    public <T> Observable.OnSubscribe<T> onSubscribeStart(Observable<? extends T> observableInstance, Observable.OnSubscribe<T> onSubscribe) {
      return ExecController.current()
        .map(e -> executionBackedOnSubscribe(onSubscribe, e))
        .orElse(onSubscribe);
    }

    private <T> Observable.OnSubscribe<T> executionBackedOnSubscribe(final Observable.OnSubscribe<T> onSubscribe, final ExecController e) {
      return (subscriber) -> onSubscribe.call(new ExecutionBackedSubscriber<>(e.getControl(), subscriber));
    }
  }

  private static class ExecutionBackedSubscriber<T> extends Subscriber<T> {
    private final Subscriber<? super T> subscriber;
    private final ExecControl execControl;

    public ExecutionBackedSubscriber(ExecControl execControl, Subscriber<? super T> subscriber) {
      super(subscriber);
      this.execControl = execControl;
      this.subscriber = subscriber;
    }

    @Override
    public void onCompleted() {
      try {
        subscriber.onCompleted();
      } catch (final OnErrorNotImplementedException e) {
        execControl.promise(f -> f.error(e.getCause())).then(Action.noop());
      }
    }

    @Override
    public void onError(Throwable e) {
      try {
        subscriber.onError(e);
      } catch (final OnErrorNotImplementedException e2) {
        execControl.promise(f -> f.error(e2.getCause())).then(Action.noop());
      }
    }

    public void onNext(T t) {
      try {
        subscriber.onNext(t);
      } catch (final OnErrorNotImplementedException e) {
        execControl.promise(fulfiller -> fulfiller.error(e.getCause())).then(Action.noop());
      }
    }
  }

}

