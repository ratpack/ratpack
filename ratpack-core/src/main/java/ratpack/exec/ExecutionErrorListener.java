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

import com.google.common.reflect.TypeToken;
import ratpack.registry.Registry;

/**
 * A handler for capturing unhandled errors in an {@link Execution}.
 *
 * <pre class="java">{@code
 * import ratpack.exec.Execution;
 * import ratpack.exec.ExecutionErrorListener;
 *
 * import static org.junit.Assert.assertTrue;
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
 *     ExecutionErrorListener listener = new RecordingExecutionErrorListener();
 *     try (ExecHarness harness = ExecHarness.harness()) {
 *       harness.run(
 *         // add our custom ExecutionErrorListener to the execution registry
 *         registrySpec ->
 *           registrySpec.add(ExecutionErrorListener.class, listener),
 *         // throw an unhandled exception
 *         execution ->
 *           throw new RuntimeException("!!")
 *       );
 *     }
 *     assertTrue(listener.getMessage(), "!!");
 *   }
 * }
 *
 * }</pre>
 *
 * @since 1.5
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
