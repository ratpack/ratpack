/*
 * Copyright 2019 the original author or authors.
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

import com.google.common.reflect.TypeToken;

/**
 * A handler for capturing unhandled errors in an {@link Execution}.
 *
 * <pre class="java">{@code
 * 
 * import ratpack.exec.Execution;
 * import ratpack.exec.ExecutionErrorListener;
 * import ratpack.test.exec.ExecHarness;
 *
 * import static org.junit.Assert.assertSame;
 *
 * public class Example {
 *   public static class RecordingExecutionErrorListener implements ExecutionErrorListener {
 *     private String message;
 *
 *     public String getMessage() {
 *       return this.message;
 *     }
 *
 *     public void onError(Execution execution, Throwable error) {
 *       this.message = error.getMessage();
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     RecordingExecutionErrorListener listener = new RecordingExecutionErrorListener();
 *     try (ExecHarness harness = ExecHarness.harness()) {
 *       ExecHarness.runSingle(
 *         // add our custom ExecutionErrorListener to the execution registry
 *         registrySpec -> { registrySpec.add(ExecutionErrorListener.class, listener);},
 *         // throw an unhandled exception
 *         execution -> {
 *         throw new RuntimeException("!!");
 *         }
 *       );
 *     } catch (RuntimeException re) {
 *         //Do nothing but don't let the Exception to kill the execution.
 *     }
 *     assertSame(listener.getMessage(), "!!");
 *   }
 * }
 *
 * }</pre>
 *
 * @since 1.7
 */
@FunctionalInterface
public interface ExecutionErrorListener {

  /**
   * A type token for this type.
   */
  TypeToken<ExecutionErrorListener> TYPE = TypeToken.of(ExecutionErrorListener.class);

  /**
   * This method is invoked when an unhandled error is thrown in an {@link Execution}.
   *
   * @param execution the {@link Execution} from which the error was thrown
   * @param error the error that was thrown
     */
  void onError(Execution execution, Throwable error) throws Exception;
}
