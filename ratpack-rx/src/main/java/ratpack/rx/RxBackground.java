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


import rx.Observable;

import java.util.concurrent.Callable;

/**
 * Similar to {@link ratpack.handling.Background} except that an {@link rx.Observable} for the background result is returned.
 * <p>
 * Use of this class for background operations is superior due to the composable nature of observables.
 * <p>
 * The {@link RxModule} provides this type.
 * </p>
 * <h4>Error handling</h4>
 * <p>
 * The observables returned by {@link #observe(Callable)} and {@link #observeEach(Callable)} are integrated into the standard Ratpack error handling mechanism.
 * Any <i>unhandled</i> error that occurs will be forwarded to the error handler of the active context at the time the background was entered into.
 * </p>
 */
public interface RxBackground {

  /**
   * Creates an {@link Observable} that will execute the given {@link Callable} when an {@link rx.Observer} subscribes to it.
   * <p>
   * The Observer's {@link rx.Observer#onNext onNext} method will be called exactly once with the result of the Callable.
   * <p>
   * <pre class="tested">
   * import ratpack.handling.Handler;
   * import ratpack.handling.Context;
   * import ratpack.rx.RxBackground;
   * import javax.inject.Inject;
   * import java.util.concurrent.Callable;
   * import rx.util.functions.Func1;
   * import rx.util.functions.Action1;
   *
   * public class ReactiveHandler implements Handler {
   *   private final RxBackground rxBackground;
   *
   *   {@literal @}Inject
   *   public ReactiveHandler(RxBackground rxBackground) {
   *     this.rxBackground = rxBackground;
   *   }
   *
   *   public void handle(Context context) {
   *     rxBackground.observe(new Callable&lt;String&gt;() {
   *       public String call() {
   *         // do some blocking IO here
   *         return "hello world";
   *       }
   *     }).map(new Func1&lt;String, String&gt;() {
   *       public String call(String input) {
   *         return input.toUpperCase();
   *       }
   *     }).subscribe(new Action1&lt;String&gt;() {
   *       public void call(String str) {
   *         context.render(str); // renders: HELLO WORLD
   *       }
   *     });
   *   }
   * }
   * </pre>
   * <p>
   * A similar example in the Groovy DSL would look like:
   * </p>
   * <pre class="groovy-chain-dsl">
   * import ratpack.rx.RxBackground
   *
   * handler { RxBackground rxBackground ->
   *   rxBackground.observe {
   *     // do some blocking IO
   *     "hello world"
   *   } map { String input ->
   *     input.toUpperCase()
   *   } subscribe {
   *     render it // renders: HELLO WORLD
   *   }
   * }
   * </pre>
   * <p>
   * As with {@link ratpack.handling.Background#exec(Callable)}, the Callable should do little more than calling a blocking operation
   * and return the value.
   * <p>
   * See the section describing error handling on {@link RxModule}
   *
   * @param callable The blocking operation
   * @param <T> The type of value returned by the blocking operation
   * @return An {@link rx.Observable} of the blocking operation outcome
   * @see RxBackground#observeEach(Callable)
   */
  <T> Observable<T> observe(Callable<T> callable);

  /**
   * Creates an {@link Observable} that will execute the given {@link Callable} when an {@link rx.Observer} subscribes to it.
   * <p>
   * The Observer's {@link rx.Observer#onNext onNext} method will be called for each item in the Callable's {@link Iterable} result.
   * <p>
   * For example, when a Callable returns a List&lt;String&gt; and a transformation of uppercasing each String in the List is required
   * then RxBackground can be used like this:
   * <pre class="groovy-chain-dsl">
   * import ratpack.rx.RxBackground
   *
   * handler { RxBackground rxBackground ->
   *   rxBackground.observeEach {
   *     // do some blocking IO and return a List&lt;String&gt;
   *     // each item in the List is emitted to the next Observable, not the List
   *     ["a", "b", "c"]
   *   } map { String input ->
   *     input.toUpperCase()
   *   } subscribe {
   *     println it
   *   }
   * }
   * </pre>
   * The output would be:
   * <br>A
   * <br>B
   * <br>C
   * <p>
   * As with {@link ratpack.handling.Background#exec(Callable)}, the Callable should do little more than calling a blocking operation
   * and return the value.
   * <p>
   * See the section describing error handling on {@link RxModule}
   *
   * @param callable The blocking operation
   * @param <I> The Iterable type of the value returned by the blocking operation
   * @param <T> The type of the Iterable I
   * @return An {@link rx.Observable} of the blocking operation outcome
   */
  <I extends Iterable<T>, T> Observable<T> observeEach(Callable<I> callable);

}

