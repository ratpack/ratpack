/*
 * Copyright 2016 the original author or authors.
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

import io.netty.channel.EventLoop;
import ratpack.func.Action;
import ratpack.registry.RegistrySpec;

/**
 * A specification for an {@link Execution}.
 *
 * @see ExecStarter
 * @see Execution#fork()
 * @since 1.4
 */
public interface ExecSpec {

  /**
   * Specify the top level error handler for the execution.
   * <p>
   * The given action will be invoked with any exceptions that are thrown and not handled.
   * <pre class="java">{@code
   * import org.junit.Assert;
   * import ratpack.exec.Execution;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     String value = ExecHarness.<String>yieldSingle(e -> Promise.async(d ->
   *         Execution.fork()
   *           .onError(t -> d.success("global error handler"))
   *           .start(e1 ->
   *               Promise.error(new RuntimeException("bang1"))
   *                 .then(v -> d.success("should not be called"))
   *           )
   *     )).getValue();
   *
   *     Assert.assertEquals("global error handler", value);
   *   }
   * }
   * }</pre>
   *
   * @param onError the top level error handler for the execution
   * @return {@code this}
   */
  ExecSpec onError(Action<? super Throwable> onError);

  /**
   * Specifies the completion callback for the execution.
   * <p>
   * The given action will effectively execute <b>outside</b> of the execution.
   * The action is expected to be synchronous and cannot perform async operations.
   * During its execution, there will be no thread bound execution or execution control.
   * Any exceptions raised will be logged.
   * <p>
   * This method should be used as a last resort.
   * <p>
   * The action will be invoked regardless of whether the execution completed with an error or not.
   * If the execution did complete with an error, the given action will be invoked <b>after</b> the error handler.
   * <p>
   * This method is not additive.
   * That is, any subsequent calls replace the previous value.

   * @param onComplete the action to invoke when the execution completes.
   * @return {@code this}
   */
  ExecSpec onComplete(Action<? super Execution> onComplete);

  /**
   * Specifies an action to be taken just before the execution starts.
   * <p>
   * The action will be invoked after {@link #register(Action)} and any {@link ExecInitializer}'s.
   * <p>
   * The {@link Execution#current() current execution} during the given callback is the
   * parent execution of the new execution being started.
   * If the execution is being created from outside of an execution,
   * there will be no current execution during the callback.
   * <p>
   * This method is not additive.
   * That is, any subsequent calls replace the previous value.

   * @param onStart the action to invoke just before the execution starts
   * @return {@code this}
   */
  ExecSpec onStart(Action<? super Execution> onStart);

  /**
   * Populates the execution's registry.
   * <p>
   * The {@link Execution#current() current execution} during the given callback is the
   * parent execution of the new execution being started.
   * If the execution is being created from outside of an execution,
   * there will be no current execution during the callback.
   * <p>
   * This method is not additive.
   * That is, any subsequent calls replace the previous value.
   *
   * @param action the initial contents of the execution's registry.
   * @return {@code this}
   */
  ExecSpec register(Action<? super RegistrySpec> action);

  /**
   * Specifies that the execution must run on the given event loop.
   * <p>
   * If this method is not called, an event loop will be automatically assigned from the {@link ExecController#getEventLoopGroup() exec controller's event loop group}.
   * It is generally not required, or desirable, to call this method.
   *
   * @param eventLoop the event loop to use for the execution
   * @return {@code this}
   */
  ExecSpec eventLoop(EventLoop eventLoop);

}
