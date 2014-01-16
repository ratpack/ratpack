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
 * Similar to {@link ratpack.background.Background} except that an {@link rx.Observable} for the background result is returned.
 * <p>
 * Use of this class for background operations is superior due to the composable nature of observables.
 * </p>
 * When the {@link RxModule} has been registered for the application, the rx background can be used like this:
 * </p>
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
 *   @Inject
 *   public ReactiveHandler(RxBackground rxBackground) {
 *     this.rxBackground = rxBackground;
 *   }
 *
 *   public void handle(Context context) {
 *     rxBackground.exec(new Callable&lt;String&gt;() {
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
 *   rxBackground.exec {
 *     // do some blocking IO
 *     "hello world"
 *   } map { String input ->
 *     input.toUpperCase()
 *   } subscribe {
 *     render it // renders: HELLO WORLD
 *   }
 * }
 * </pre>
 * <h4>Error handling</h4>
 * <p>
 * The observables returned by {@link #exec(Callable)} are integrated into the error handling mechanism.
 * Any <i>unhandled</i> error that occurs will be forwarded to the error handler of the active context at the time the background was entered into.
 * </p>
 */
public interface RxBackground {

  /**
   * Executes the given callable in the background, providing an observable for the result.
   * <p>
   * As with {@link ratpack.background.Background#exec(Callable)}, the callable should do little more than calling a blocking operation
   * and return the value.
   * <p>
   * See the section describing error handling on {@link RxModule}
   *
   * @param callable The blocking operation
   * @param <T> The type of value returned by the blocking operation
   * @return An {@link rx.Observable} of the blocking operation outcome
   */
  <T> Observable<T> exec(Callable<T> callable);

}
