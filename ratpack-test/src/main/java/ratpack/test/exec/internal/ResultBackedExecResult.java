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

package ratpack.test.exec.internal;

import ratpack.exec.Result;
import ratpack.test.exec.ExecResult;

public class ResultBackedExecResult<T> implements ExecResult<T> {

  private final Result<T> result;

  public ResultBackedExecResult(Result<T> result) {
    this.result = result;
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
  public boolean isFailure() {
    return result.isFailure();
  }

  @Override
  public T getValueOrThrow() throws Exception {
    return result.getValueOrThrow();
  }

  @Override
  public String toString() {
    return "ExecResult{complete=false, failure=" + getThrowable() + ", value=" + getValue() + '}';
  }
}
