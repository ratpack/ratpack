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

package ratpack.exec;

import com.google.common.util.concurrent.ListeningExecutorService;
import ratpack.promise.SuccessOrErrorPromise;

import java.util.concurrent.Callable;

/**
 * The background execution space for blocking operations.
 * <p>
 * The background is implicitly bound to the currently in use {@link ratpack.handling.Context} on the current thread.
 *
 * @see #exec(Callable)
 * @see ratpack.handling.Context#background(Callable)
 * @see ratpack.handling.Context#getBackground()
 */
public interface Background {

  /**
   * Execute the given operation in the background, returning a promise for its result.
   * <p>
   * This method executes asynchronously, in that it does not invoke the {@code operation} before returning the promise.
   * When the returned promise is subscribed to (i.e. its {@link ratpack.promise.SuccessPromise#then(ratpack.func.Action)} method is called),
   * the given {@code operation} will be submitted to a thread pool that is different to the request handling thread pool.
   * Therefore, if the returned promise is never subscribed to, the {@code operation} will never be initiated.
   * <p>
   * The promise returned by this method, have the same default error handling strategy as those returned by {@link ratpack.handling.Context#promise(ratpack.func.Action)}.
   * <p>
   * <pre class="tested">
   * import ratpack.handling.*;
   * import ratpack.func.Action;

   * import java.util.concurrent.Callable;
   *
   * public class BackgroundUsingJavaHandler implements Handler {
   *   void handle(final Context context) {
   *     context.background(new Callable&lt;String&gt;() {
   *        public String call() {
   *          // perform some kind of blocking IO in here, such as accessing a database
   *          return "hello world, from the background!";
   *        }
   *     }).then(new Action&lt;String&gt;() {
   *       public void execute(String result) {
   *         context.render(result);
   *       }
   *     });
   *   }
   * }
   *
   * public class BackgroundUsingGroovyHandler implements Handler {
   *   void handle(final Context context) {
   *     context.background {
   *       "hello world, from the background!"
   *     } then { String result ->
   *       context.render(result)
   *     }
   *   }
   * }
   *
   * // Test (Groovy) &hellip;
   *
   * import ratpack.test.embed.PathBaseDirBuilder
   * import ratpack.groovy.test.TestHttpClients
   * import ratpack.groovy.test.embed.ClosureBackedEmbeddedApplication
   *
   * def baseDir = new PathBaseDirBuilder(new File("some/path"))
   * def app = new ClosureBackedEmbeddedApplication(baseDir)
   *
   * app.handlers {
   *   get("java", new BackgroundUsingJavaHandler())
   *   get("groovy", new BackgroundUsingGroovyHandler())
   * }
   *
   * def client = TestHttpClients.testHttpClient(app)
   *
   * assert client.getText("java") == "hello world, from the background!"
   * assert client.getText("groovy") == "hello world, from the background!"
   *
   * app.close()
   * </pre>
   *
   * @param operation The operation to perform
   * @param <T> The type of result object that the operation produces
   * @return a promise for the return value of the callable.
   */
  <T> SuccessOrErrorPromise<T> exec(Callable<T> operation);

  ListeningExecutorService getExecutor();

}
