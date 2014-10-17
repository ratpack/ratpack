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

import ratpack.test.exec.ExecResult;

public class CompleteExecResult<T> implements ExecResult<T> {

  @Override
  public boolean isComplete() {
    return true;
  }

  @Override
  public Throwable getThrowable() {
    return null;
  }

  @Override
  public T getValue() {
    return null;
  }

  @Override
  public boolean isSuccess() {
    return false;
  }

  @Override
  public boolean isFailure() {
    return false;
  }

  @Override
  public T getValueOrThrow() throws Exception {
    return null;
  }

}
