/*
 * Copyright 2018 the original author or authors.
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

package ratpack.reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.reactor.internal.BlockingExecutorBackedScheduler;
import ratpack.reactor.internal.DefaultSchedulers;
import ratpack.reactor.internal.ErrorHandler;
import ratpack.reactor.internal.ExecControllerBackedScheduler;
import ratpack.registry.RegistrySpec;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.util.Exceptions;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * Provides integration with <a href="https://projectreactor.io/">Project Reactor</a>.
 * <p>
 * The methods of this class provide bi-directional conversion between Ratpack's {@link Promise} and Reactor's {@link Flux} and {@link Mono}.
 * This allows Ratpack promise based API to be integrated into an RxJava based app and vice versa.
 * <p>
 * To test observable based services that use Ratpack's execution semantics, use the {@code ExecHarness} and convert the observable back to a promise with {@link #promise(Flux)}.
 * <p>
 * The methods in this class are also provided as Kotlin Extensions.
 * When using Groovy, each static method in this class is able to act as an instance-level method against the {@link Flux} type.
 */
public abstract class ReactorRatpack {

  private ReactorRatpack() {
  }

  /**
   * Registers an {@link Hooks#onOperatorError(BiFunction)} with Reactor that provides a default error handling strategy of forwarding exceptions to the execution error handler.
   * <p>
   * This method is idempotent.
   * It only needs to be called once per JVM, regardless of how many Ratpack applications are running within the JVM.
   * <p>
   * For a Java application, a convenient place to call this is in the handler factory implementation.
   * <pre class="java">{@code
   * import ratpack.error.ServerErrorHandler;
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.test.embed.EmbeddedApp;
   * import rx.Observable;
   * import static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static void main(String... args) throws Exception {
   * ReactorRatpack.initialize(); // must be called once for the life of the JVM
   * <p>
   * EmbeddedApp.fromHandlers(chain -> chain
   * .register(s -> s
   * .add(ServerErrorHandler.class, (ctx, throwable) ->
   * ctx.render("caught by error handler: " + throwable.getMessage())
   * )
   * )
   * .get(ctx -> Observable.<String>error(new Exception("!")).subscribe(ctx::render))
   * ).test(httpClient ->
   * assertEquals("caught by error handler: !", httpClient.getText())
   * );
   * }
   * }
   * }</pre>
   */
  @SuppressWarnings("unchecked")
  public static void initialize() {
    Hooks.onOperatorError(new ErrorHandler());
  }

  /**
   * Converts a {@link Operation} into a {@link Flux}.
   * <p>
   * The returned flux emits completes upon completion of the operation without emitting a value, and emits the error (i.e. via {@code errer()}) if it fails.
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.exec.Operation;
   * import ratpack.test.exec.ExecHarness;
   * <p>
   * import public static org.junit.Assert.assertTrue;
   * <p>
   * public class Example {
   * public static boolean executed;
   * public static void main(String... args) throws Exception {
   * ExecHarness.runSingle(e ->
   * Operation.of(() -> executed = true)
   * .to(ReactorRatpack::observe)
   * .subscribe()
   * );
   * <p>
   * assertTrue(executed);
   * }
   * }
   * }</pre>
   *
   * @param operation the operation
   * @return an observable for the operation
   */
  public static Flux<Void> flux(Operation operation) {
    return Flux.create(sink -> operation.onError(sink::error).then(sink::complete));
  }

  /**
   * Converts a {@link Promise} into an {@link Flux}.
   * <p>
   * The returned observable emits the promise's single value if it succeeds, and emits the error (i.e. via {@code onError()}) if it fails.
   * <p>
   * This method works well as a method reference to the {@link Promise#to(ratpack.func.Function)} method.
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   * <p>
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static String value;
   * public static void main(String... args) throws Exception {
   * ExecHarness.runSingle(e ->
   * Promise.value("hello world")
   * .to(ReactorRatpack::observe)
   * .map(String::toUpperCase)
   * .subscribe(s -> value = s)
   * );
   * <p>
   * assertEquals("HELLO WORLD", value);
   * }
   * }
   * }</pre>
   *
   * @param promise the promise
   * @param <T>     the type of value promised
   * @return an observable for the promised value
   */
  public static <T> Flux<T> flux(Promise<T> promise) {
    return Flux.create(subscriber ->
      promise.onError(subscriber::error).then(value -> {
        subscriber.next(value);
        subscriber.complete();
      })
    );
  }

  /**
   * Converts a {@link Promise} for an iterable into an {@link Flux}.
   * <p>
   * The promised iterable will be emitted to the observer one element at a time, like {@link Flux#fromIterable(Iterable)}.
   * <p>
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   * <p>
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.List;
   * <p>
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static void main(String... args) throws Exception {
   * final List<String> items = new LinkedList<>();
   * ExecHarness.runSingle(e ->
   * Promise.value(Arrays.asList("foo", "bar"))
   * .to(ReactorRatpack::observeEach)
   * .subscribe(items::add)
   * );
   * <p>
   * assertEquals(Arrays.asList("foo", "bar"), items);
   * }
   * }
   * }</pre>
   *
   * @param promise the promise
   * @param <T>     the element type of the promised iterable
   * @param <I>     the type of iterable
   * @return an observable for each element of the promised iterable
   * @see #flux(ratpack.exec.Promise)
   */
  public static <T, I extends Iterable<T>> Flux<T> fluxEach(Promise<I> promise) {
    return Flux.merge(flux(promise).map(Flux::fromIterable));
  }

  /**
   * Converts a {@link Promise} into a {@link Mono}.
   * <p>
   * The returned Single emits the promise's single value if it succeeds, and emits the error (i.e. via {@code onError()}) if it fails.
   * <p>
   * This method works well as a method reference to the {@link Promise#to(ratpack.func.Function)} method.
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   * <p>
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static String value;
   * public static void main(String... args) throws Exception {
   * ExecHarness.runSingle(e ->
   * Promise.value("hello world")
   * .to(ReactorRatpack::mono)
   * .map(String::toUpperCase)
   * .subscribe(s -> value = s)
   * );
   * <p>
   * assertEquals("HELLO WORLD", value);
   * }
   * }
   * }</pre>
   *
   * @param promise the promise
   * @param <T>     the type of value promised
   * @return a single for the promised value
   */
  public static <T> Mono<T> mono(Promise<T> promise) {
    return Mono.create(subscriber ->
      promise.onError(subscriber::error).then(subscriber::success)
    );
  }

  /**
   * Converts a {@link Flux} into a {@link Promise}, for all of the observable's items.
   * <p>
   * This method can be used to simply adapt an observable to a promise, but can also be used to bind an observable to the current execution.
   * <p>
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.test.exec.ExecHarness;
   * import reactor.core.publisher.flux;
   * import java.util.List;
   * import java.util.Arrays;
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static class AsyncService {
   * public <T> Flux<T> flux(final T value) {
   * return Flux.create(subscriber ->
   * new Thread(() -> {
   * subscriber.next(value);
   * subscriber.complete();
   * }).start()
   * );
   * }
   * }
   * <p>
   * public static void main(String[] args) throws Throwable {
   * List<String> results = ExecHarness.yieldSingle(execution ->
   * ReactorRatpack.promise(new AsyncService().flux("foo"))
   * ).getValue();
   * <p>
   * assertEquals(Arrays.asList("foo"), results);
   * }
   * }
   * }</pre>
   * <p>
   * <p>
   * This method uses {@link Flux#collectList()} to collect the observable's contents into a list.
   * It therefore should not be used with observables with many or infinite items.
   * <p>
   * If it is expected that the observable only emits one element, it is typically more convenient to use {@link #promiseSingle(Mono)}.
   * <p>
   * If the observable emits an error, the returned promise will fail with that error.
   * <p>
   * This method must be called during an execution.
   *
   * @param flux the flux
   * @param <T>        the type of the value observed
   * @return a promise that returns all values from the observable
   * @throws UnmanagedThreadException if called outside of an execution
   * @see #promiseSingle(Mono)
   */
  public static <T> Promise<List<T>> promise(Flux<T> flux) throws UnmanagedThreadException {
    return Promise.async(f -> flux.collectList().subscribe(f::success, f::error));
  }

  /**
   * Converts a {@link Mono} into a {@link Promise}, for the mono's single item.
   * <p>
   * This method can be used to simply adapt an observable to a promise, but can also be used to bind an observable to the current execution.
   * <p>
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.test.exec.ExecHarness;
   * import reactor.core.publisher.Mono;
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static class AsyncService {
   * public <T> Observable<T> observe(final T value) {
   * return Mono.create(subscriber ->
   * new Thread(() -> {
   * subscriber.success(value);
   * subscriber.onCompleted();
   * }).start()
   * );
   * }
   * }
   * <p>
   * public static void main(String[] args) throws Throwable {
   * String result = ExecHarness.yieldSingle(execution ->
   * ReactorRatpack.promiseSingle(new AsyncService().observe("foo"))
   * ).getValue();
   * <p>
   * assertEquals("foo", result);
   * }
   * }
   * }</pre>
   * <p>
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.test.exec.ExecHarness;
   * import reactor.core.publisher.Flux;
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static void main(String[] args) throws Throwable {
   * String result = ExecHarness.yieldSingle(execution ->
   * ReactorRatpack.promiseSingle(Mono.<String>just("foo"))
   * ).getValue();
   * assertEquals("foo", result);
   * }
   * }
   * }</pre>
   * <p>
   * <p>
   * If it is expected that the observable may emit more than one element, use {@link #promise(Flux)}.
   * <p>
   * If the observable emits an error, the returned promise will fail with that error.
   * If the observable emits no items, the returned promise will fail with a {@link java.util.NoSuchElementException}.
   * If the observable emits more than one item, the returned promise will fail with an {@link IllegalStateException}.
   * <p>
   * This method must be called during an execution.
   *
   * @param mono the mono
   * @param <T>        the type of the value observed
   * @return a promise that returns the sole value from the observable
   * @see #promise(Flux)
   */
  public static <T> Promise<T> promiseSingle(Mono<T> mono) throws UnmanagedThreadException {
    return Promise.async(f -> mono.subscribe(f::success, f::error));
  }

  /**
   * Converts a {@link Flux} into a {@link Publisher}, for all of the observable's items.
   * <p>
   * This method can be used to simply adapt an observable to a ReactiveStreams publisher.
   * <p>
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.stream.Streams;
   * import ratpack.test.exec.ExecHarness;
   * import reactor.core.publisher.Flux;
   * import java.util.List;
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static class AsyncService {
   * public <T> Observable<T> flux(final T value) {
   * return Flux.create(subscriber ->
   * new Thread(() -> {
   * subscriber.next(value);
   * subscriber.complete();
   * }).start()
   * );
   * }
   * }
   * <p>
   * public static void main(String[] args) throws Throwable {
   * List<String> result = ExecHarness.yieldSingle(execution ->
   * ReactorRatpack.publisher(new AsyncService().flux("foo")).toList()
   * ).getValue();
   * assertEquals("foo", result.get(0));
   * }
   * }
   * }</pre>
   *
   * @param flux the flux
   * @param <T>        the type of the value observed
   * @return a ReactiveStreams publisher containing each value of the flux
   */
  public static <T> TransformablePublisher<T> publisher(Flux<T> flux) {
    return Streams.transformable(flux);
  }

  /**
   * Binds the given flux to the current execution, allowing integration of third-party asynchronous fluxes with Ratpack's execution model.
   * <p>
   * This method is useful when you want to consume an asynchronous flux within a Ratpack execution, as a flux.
   * It is just a combination of {@link #promise(Flux)} and {@link #fluxEach(Promise)}.
   * <p>
   * <pre class="java">{@code
   * import reactor.core.publisher.Flux;
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.reactor.ReactorRatpack;
   * import java.util.Arrays;
   * import java.util.List;
   * import public static org.junit.Assert.*;
   * <p>
   * public class Example {
   * public static void main(String... args) throws Exception {
   * Flux<String> asyncFlux = Flux.create(subscriber ->
   * new Thread(() -> {
   * subscriber.next("foo");
   * subscriber.next("bar");
   * subscriber.complete();
   * }).start()
   * );
   * <p>
   * List<String> strings = ExecHarness.yieldSingle(e ->
   * ReactorRatpack.promise(asyncFlux.compose(ReactorRatpack::bindExec))
   * ).getValue();
   * <p>
   * assertEquals(Arrays.asList("foo", "bar"), strings);
   * }
   * }
   * }</pre>
   * <p>
   *
   * @param source the observable source
   * @param <T>    the type of item observed
   * @return an observable stream equivalent to the given source
   * @see #fluxEach(Promise)
   * @see #promise(Flux)
   */
  public static <T> Flux<T> bindExec(Flux<T> source) {
    return Exceptions.uncheck(() -> promise(source).to(ReactorRatpack::fluxEach));
  }

  /**
   * Parallelize a flux by forking it's execution onto a different Ratpack compute thread and automatically binding
   * the result back to the original execution.
   * <p>
   * This method can be used for simple parallel processing.  It's behavior is similar to the
   * <a href="http://reactivex.io/documentation/operators/subscribeon.html">subscribeOn</a> but allows the use of
   * Ratpack compute threads.  Using <code>fork</code> modifies the execution of the upstream observable.
   * <p>
   * This is different than <code>forkEach</code> which modifies where the downstream is executed.
   * <p>
   * <pre class="java">{@code
   * import ratpack.func.Pair;
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.test.exec.ExecHarness;
   * <p>
   * import reactor.core.publisher.Flux;
   * <p>
   * import public static org.junit.Assert.assertEquals;
   * import public static org.junit.Assert.assertNotEquals;
   * <p>
   * public class Example {
   * public static void main(String[] args) throws Exception {
   * ReactorRatpack.initialize();
   * <p>
   * try (ExecHarness execHarness = ExecHarness.harness(6)) {
   * Integer sum = execHarness.yield(execution -> {
   * final String originalComputeThread = Thread.currentThread().getName();
   * <p>
   * Flux<Integer> unforkedFlux = Flux.just(1);
   * <p>
   * // `map` is executed upstream from the fork; that puts it on another parallel compute thread
   * Flux<Pair<Integer, String>> forkedFlux = Flux.just(2)
   * .map((val) -> Pair.of(val, Thread.currentThread().getName()))
   * .compose(ReactorRatpack::fork);
   * <p>
   * return ReactorRatpack.promiseSingle(
   * Flux.zip(unforkedFlux, forkedFlux, (Integer intVal, Pair<Integer, String> pair) -> {
   * String forkedComputeThread = pair.right;
   * assertNotEquals(originalComputeThread, forkedComputeThread);
   * return intVal + pair.left;
   * })
   * );
   * }).getValueOrThrow();
   * <p>
   * assertEquals(sum.intValue(), 3);
   * }
   * }
   * }
   * }</pre>
   *
   * @param observable the observable sequence to execute on a different compute thread
   * @param <T>        the element type
   * @return an observable on the compute thread that <code>fork</code> was called from
   * @see #forkEach(Flux)
   * @since 1.4
   */
  public static <T> Flux<T> fork(Flux<T> observable) {
    return fluxEach(promise(observable).fork());
  }

  /**
   * A variant of {@link #fork} that allows access to the registry of the forked execution inside an {@link Action}.
   * <p>
   * This allows the insertion of objects via {@link RegistrySpec#add} that will be available to the forked flux.
   * <p>
   * You do not have access to the original execution inside the {@link Action}.
   * <p>
   * <pre class="java">{@code
   * import ratpack.exec.Execution;
   * import ratpack.registry.RegistrySpec;
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.test.exec.ExecHarness;
   * <p>
   * import reactor.core.publisher.Flux;
   * <p>
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static void main(String[] args) throws Exception {
   * ReactorRatpack.initialize();
   * <p>
   * try (ExecHarness execHarness = ExecHarness.harness(6)) {
   * String concatenatedResult = execHarness.yield(execution -> {
   * <p>
   * Observable<String> notYetForked = Observable.just("foo")
   * .map((value) -> value + Execution.current().get(String.class));
   * <p>
   * Observable<String> forkedObservable = ReactorRatpack.fork(
   * notYetForked,
   * (RegistrySpec registrySpec) -> registrySpec.add("bar")
   * );
   * <p>
   * return ReactorRatpack.promiseSingle(forkedObservable);
   * }).getValueOrThrow();
   * <p>
   * assertEquals(concatenatedResult, "foobar");
   * }
   * }
   * }
   * }</pre>
   *
   * @param flux         the flux sequence to execute on a different compute thread
   * @param doWithRegistrySpec an Action where objects can be inserted into the registry of the forked execution
   * @param <T>                the element type
   * @return an observable on the compute thread that <code>fork</code> was called from
   * @throws Exception
   * @see #fork(Flux)
   * @since 1.4
   */
  public static <T> Flux<T> fork(Flux<T> flux, Action<? super RegistrySpec> doWithRegistrySpec) throws Exception {
    return fluxEach(promise(flux).fork(execSpec -> execSpec.register(doWithRegistrySpec)));
  }


  /**
   * Parallelize an observable by creating a new Ratpack execution for each element.
   * <p>
   * <pre class="java">{@code
   * import ratpack.reactor.ReactorRatpack;
   * import ratpack.util.Exceptions;
   * import ratpack.test.exec.ExecHarness;
   * <p>
   * import reactor.core.publisher.Flux;
   * <p>
   * import java.util.List;
   * import java.util.Arrays;
   * import java.util.LinkedList;
   * import java.util.Collection;
   * import java.util.Collections;
   * import java.util.concurrent.CyclicBarrier;
   * <p>
   * import public static org.junit.Assert.assertEquals;
   * <p>
   * public class Example {
   * public static void main(String[] args) throws Exception {
   * ReactorRatpack.initialize();
   * <p>
   * CyclicBarrier barrier = new CyclicBarrier(5);
   * <p>
   * try (ExecHarness execHarness = ExecHarness.harness(6)) {
   * List<Integer> values = execHarness.yield(execution ->
   * ReactorRatpack.promise(
   * Flux.just(1, 2, 3, 4, 5)
   * .compose(ReactorRatpack::forkEach) // parallelize
   * .doOnNext(value -> Exceptions.uncheck(() -> barrier.await())) // wait for all values
   * .map(integer -> integer.intValue() * 2)
   * .serialize()
   * )
   * ).getValue();
   * <p>
   * List<Integer> sortedValues = new LinkedList<>(values);
   * Collections.sort(sortedValues);
   * assertEquals(Arrays.asList(2, 4, 6, 8, 10), sortedValues);
   * }
   * }
   * }
   * }</pre>
   *
   * @param flux the observable sequence to process each element of in a forked execution
   * @param <T>        the element type
   * @return an observable
   */
  public static <T> Flux<T> forkEach(Flux<T> flux) {
    return forkEach(flux, Action.noop());
  }

  /**
   * A variant of {@link #forkEach} that allows access to the registry of each forked execution inside an {@link Action}.
   * <p>
   * This allows the insertion of objects via {@link RegistrySpec#add} that will be available to every forked flux.
   * <p>
   * You do not have access to the original execution inside the {@link Action}.
   *
   * @param flux         the flux sequence to process each element of in a forked execution
   * @param doWithRegistrySpec an Action where objects can be inserted into the registry of the forked execution
   * @param <T>                the element type
   * @return an observable
   * @see #forkEach(Flux)
   * @see #fork(Flux, Action)
   * @since 1.4
   */
  public static <T> Flux<T> forkEach(Flux<T> flux, Action<? super RegistrySpec> doWithRegistrySpec) {

    return flux.transform(Operators.lift((Scannable scannable, CoreSubscriber<? super T> subscriber) ->
      new CoreSubscriber<T>() {

        private final AtomicInteger wip = new AtomicInteger(1);
        private final AtomicBoolean closed = new AtomicBoolean();
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
          this.subscription = s;
          s.request(1);
          subscriber.onSubscribe(s);
        }

        @Override
        public void onComplete() {
          maybeDone();
        }

        @Override
        public void onError(final Throwable e) {
          terminate(() -> subscriber.onError(e));
        }

        private void maybeDone() {
          if (wip.decrementAndGet() == 0) {
            terminate(subscriber::onComplete);
          }
        }

        private void terminate(Runnable runnable) {
          if (closed.compareAndSet(false, true)) {
            subscription.cancel();
            runnable.run();
          }
        }

        @Override
        public void onNext(final T t) {
          // Avoid the overhead of creating executions if downstream is no longer interested
          if (closed.get()) {
            return;
          }

          wip.incrementAndGet();
          Execution.fork()
            .register(doWithRegistrySpec)
            .onComplete(e -> this.maybeDone())
            .onError(this::onError)
            .start(e -> {
              if (!closed.get()) {
                subscription.request(1);
                subscriber.onNext(t);
              }
            });
        }
      }
    ));
  }

  /**
   * A scheduler that uses the application event loop and initialises each job as an {@link ratpack.exec.Execution} (via {@link ExecController#fork()}).
   *
   * @param execController the execution controller to back the scheduler
   * @return a scheduler
   */
  public static Scheduler computationScheduler(ExecController execController) {
    return new ExecControllerBackedScheduler(execController);
  }

  /**
   * A scheduler that uses the application event loop and initialises each job as an {@link ratpack.exec.Execution} (via {@link ExecController#fork()}).
   *
   * @return a scheduler
   */
  public static Scheduler computationScheduler() {
    return DefaultSchedulers.getComputationScheduler();
  }

  /**
   * A scheduler that uses the application io thread pool.
   *
   * @param execController the execution controller to back the scheduler
   * @return a scheduler
   */
  public static Scheduler ioScheduler(ExecController execController) {
    return new BlockingExecutorBackedScheduler(execController);
  }

  /**
   * A scheduler that uses the application io thread pool.
   *
   * @return a scheduler
   */
  public static Scheduler ioScheduler() {
    return DefaultSchedulers.getIoScheduler();
  }


}

