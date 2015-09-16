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

package ratpack.exec.internal;

import ratpack.exec.Result;
import ratpack.exec.ExecResult;
import ratpack.registry.Registry;

public class ResultBackedExecResult<T> implements ExecResult<T> {

  private final Result<T> result;
  private final Registry registry;

  public ResultBackedExecResult(Result<T> result, Registry registry) {
    this.result = result;
    this.registry = registry;
  }

  @Override
  public Registry getRegistry() {
    return registry;
  }

  @Override
  public boolean isComplete() {
    return false;
  }

  @Override
  public Throwable getThrowable() {
    return result.getThrowable();
  }

  @Override
  public T getValue() {
    return result.getValue();
  }

  @Override
  public boolean isSuccess() {
    return result.isSuccess();
  }

  @Override
  public boolean isError() {
    return result.isError();
  }

  @Override
  public T getValueOrThrow() throws Exception {
    return result.getValueOrThrow();
  }

  @Override
  public String toString() {
    return "ExecResult{complete=false, error=" + getThrowable() + ", value=" + getValue() + '}';
  }
}
