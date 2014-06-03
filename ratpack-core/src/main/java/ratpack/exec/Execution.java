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

package ratpack.exec;

import ratpack.func.Action;
import ratpack.registry.MutableRegistry;

/**
 * Represents the logical execution of task, started by {@link ExecController#start(ratpack.func.Action)}.
 * <p>
 * The handling of a request is an example of a logical execution.
 * <p>
 * An execution can be asynchronous, and may be executed by more than one thread, but never in parallel.
 * Ratpack serializes the execution segments of an execution.
 */
public interface Execution extends MutableRegistry, ExecControl {

  void setErrorHandler(Action<? super Throwable> errorHandler);

  void addInterceptor(ExecInterceptor execInterceptor, Action<? super Execution> continuation) throws Exception;

  ExecController getController();

  void onComplete(Runnable runnable);

}
