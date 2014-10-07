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
import ratpack.exec.Fulfiller;
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
import rx.internal.operators.OperatorSingle;
import rx.plugins.RxJavaObservableExecutionHook;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static ratpack.util.ExceptionUtils.toException;

/**
 * Provides integration with <a href="https://github.com/Netflix/RxJava">RxJava</a>.
 * <p>
 * <b>IMPORTANT:</b> the {@link #initialize()} method must be called to fully enable integration.
 */
public abstract class RxRatpack {

  private static ExecControl getExecControl() {
    return ExecController.current().map(ExecController::getControl).orElse(null);
  }

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
   * import ratpack.test.UnitTest;
   * import ratpack.test.handling.HandlingResult;
   * import rx.Observable;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     RxRatpack.initialize(); // must be called once per JVM
   *
   *     HandlingResult result = UnitTest.requestFixture().handleChain(chain -> {
   *       chain.register(registry ->
   *         registry.add(ServerErrorHandler.class, (context, throwable) ->
   *           context.render("caught by error handler: " + throwable.getMessage())
   *         )
   *       );
   *
   *       chain.get(ctx -> Observable.<String>error(new Exception("!")).subscribe((s) -> {}));
   *     });
   *
   *     assert result.rendered(String.class).equals("caught by error handler: !");
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
   * Forks the current execution in order to subscribe to the given source, then joining the original execution with the source values.
   * <p>
   * This method supports parallelism in the observable stream.
   * <p>
   * This method uses {@link rx.Observable#toList()} on the given source to collect all values before returning control to the original execution.
   * As such, {@code source} should not be an infinite or extremely large stream.
   *
   * @param execControl the execution control
   * @param source the observable source
   * @param <T> the type of item observed
   * @return an observable stream equivalent to the given source
   */
  public static <T> Observable<T> forkAndJoin(final ExecControl execControl, final Observable<T> source) {
    Promise<List<T>> promise = execControl.promise(fulfiller -> execControl.exec().start(execution -> source
      .toList()
      .subscribe(new Subscriber<List<T>>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
          fulfiller.error(e);
        }

        @Override
        public void onNext(List<T> ts) {
          fulfiller.success(ts);
        }
      })));

    return observeEach(promise);
  }

  /**
   * Converts a Ratpack promise into an Rx observable.
   * <p>
   * For example, this can be used to observe blocking operations.
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.handling.HandlingResult;
   *
   * import static ratpack.rx.RxRatpack.observe;
   * import static ratpack.test.UnitTest.requestFixture;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     HandlingResult result = requestFixture().handle(context -> {
   *       Promise<String> promise = context.blocking(() -> "hello world");
   *       observe(promise).map(String::toUpperCase).subscribe(context::render);
   *     });
   *
   *     assert result.rendered(String.class).equals("HELLO WORLD");
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
   * Converts a Ratpack promise of an iterable value into an Rx observable for each element of the promised iterable.
   * <p>
   * The promised iterable will be emitted to the observer one element at a time.
   * For example, this can be used to observe background operations that produce some kind of iterable&hellip;
   *
   * <pre class="java">{@code
   * import ratpack.exec.Promise;
   * import ratpack.test.handling.HandlingResult;
   *
   * import java.util.Arrays;
   * import java.util.List;
   *
   * import static ratpack.rx.RxRatpack.observeEach;
   * import static ratpack.test.UnitTest.requestFixture;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     HandlingResult result = requestFixture().handle(context -> {
   *       Promise<List<String>> promise = context.blocking(() -> Arrays.asList("hello", "world"));
   *       observeEach(promise)
   *         .map(String::toUpperCase)
   *         .toList()
   *         .subscribe(strings -> context.render(String.join(" ", strings)));
   *     });
   *
   *     assert result.rendered(String.class).equals("HELLO WORLD");
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
   * Alternative method for forking the execution to process each observable element.
   * <p>
   * This method is alternative to {@link #forkOnNext(ExecControl)} and is functionally equivalent.
   *
   * @param execControl the execution control to use to fork executions
   * @param observable the observable sequence to process each element of in a forked execution
   * @param <T> the element type
   * @return an observable
   */
  public static <T> Observable<T> forkOnNext(ExecControl execControl, Observable<T> observable) {
    return observable.lift(RxRatpack.<T>forkOnNext(execControl));
  }

  /**
   * An operator to parallelize an observable stream by forking a new execution for each omitted item.
   * This allows downstream processing to occur in concurrent executions.
   * <p>
   * To be used with the {@link Observable#lift(Observable.Operator)} method.
   * <p>
   * The {@code onCompleted()} or {@code onError()} downstream methods are guaranteed to be called <strong>after</strong> the last item has been given to the downstream {@code onNext()} method.
   * That is, the last invocation of the downstream {@code onNext()} will have returned before {@code onCompleted()} or {@code onError()} are invoked.
   * <p>
   * This is generally a more performant alternative to using plain Rx parallelization due to Ratpack's {@link ratpack.exec.Execution} semantics and use of Netty's event loop to schedule work.
   * <pre class="java">
   * import ratpack.rx.RxRatpack;
   * import ratpack.exec.ExecController;
   * import ratpack.launch.LaunchConfigBuilder;
   *
   * import rx.Observable;
   * import rx.functions.Func1;
   * import rx.functions.Action1;
   *
   * import java.util.List;
   * import java.util.Arrays;
   * import java.util.concurrent.CyclicBarrier;
   * import java.util.concurrent.BrokenBarrierException;
   *
   * public class Example {
   *
   *   public static void main(String[] args) throws Exception {
   *     RxRatpack.initialize();
   *
   *     final CyclicBarrier barrier = new CyclicBarrier(5);
   *     final ExecController execController = LaunchConfigBuilder.noBaseDir().threads(6).build().getExecController();
   *
   *     Integer[] myArray = {1, 2, 3, 4, 5};
   *     Observable&lt;Integer&gt; source = Observable.from(myArray);
   *     List&lt;Integer&gt; doubledAndSorted = source
   *       .lift(RxRatpack.&lt;Integer&gt;forkOnNext(execController.getControl()))
   *       .map(new Func1&lt;Integer, Integer&gt;() {
   *         public Integer call(Integer integer) {
   *           try {
   *             barrier.await(); // prove stream is processed concurrently
   *           } catch (InterruptedException | BrokenBarrierException e) {
   *             throw new RuntimeException(e);
   *           }
   *           return integer.intValue() * 2;
   *         }
   *       })
   *       .serialize()
   *       .toSortedList()
   *       .toBlocking()
   *       .single();
   *
   *     try {
   *       assert doubledAndSorted.equals(Arrays.asList(2, 4, 6, 8, 10));
   *     } finally {
   *       execController.close();
   *     }
   *   }
   * }
   * </pre>
   *
   * @param execControl the execution control to use to fork executions
   * @param <T> the type of item in the stream
   * @return an observable operator
   */
  public static <T> Observable.Operator<T, T> forkOnNext(final ExecControl execControl) {
    return downstream -> new Subscriber<T>(downstream) {

      private final AtomicInteger wip = new AtomicInteger(1);

      private volatile Runnable onDone;

      @Override
      public void onCompleted() {
        onDone = downstream::onCompleted;
        maybeDone();
      }

      @Override
      public void onError(final Throwable e) {
        onDone = () -> downstream.onError(e);
        maybeDone();
      }

      private void maybeDone() {
        if (wip.decrementAndGet() == 0) {
          onDone.run();
        }
      }

      @Override
      public void onNext(final T t) {
        // Avoid the overhead of creating executions if downstream is no longer interested
        if (isUnsubscribed()) {
          return;
        }

        wip.incrementAndGet();
        execControl.exec()
          .onError(this::onError)
          .start(execution -> {
            try {
              downstream.onNext(t);
            } finally {
              maybeDone();
            }
          });
      }
    };
  }

  public static <T> Subscriber<? super T> subscriber(final Fulfiller<T> fulfiller) {
    return new OperatorSingle<T>().call(new Subscriber<T>() {
      @Override
      public void onCompleted() {

      }

      @Override
      public void onError(Throwable e) {
        fulfiller.error(e);
      }

      @Override
      public void onNext(T t) {
        fulfiller.success(t);
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
    public <T> Observable.OnSubscribe<T> onSubscribeStart(Observable<? extends T> observableInstance, final Observable.OnSubscribe<T> onSubscribe) {
      final ExecControl execControl = getExecControl();
      if (execControl != null) {
        return new Observable.OnSubscribe<T>() {
          @Override
          public void call(final Subscriber<? super T> subscriber) {
            onSubscribe.call(new ExecutionBackedSubscriber<>(execControl, subscriber));
          }
        };
      } else {
        return onSubscribe;
      }
    }
  }

  private static class ExecutionBackedSubscriber<T> extends Subscriber<T> {
    private final Subscriber<? super T> subscriber;
    private final ExecControl execControl;

    public ExecutionBackedSubscriber(ExecControl execControl, Subscriber<? super T> subscriber) {
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

