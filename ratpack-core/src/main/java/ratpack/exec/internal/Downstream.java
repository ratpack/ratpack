/*
 * Copyright 2015 the original author or authors.
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

import ratpack.exec.ExecResult;
import ratpack.exec.Result;

public interface Downstream<T> {

  public void success(T value);

  public void error(Throwable throwable);

  public void complete();

  default void accept(ExecResult<? extends T> result) {
    if (result.isComplete()) {
      complete();
    } else if (result.isFailure()) {
      error(result.getThrowable());
    } else {
      success(result.getValue());
    }
  }

  default void accept(Result<? extends T> result) {
    if (result.isFailure()) {
      error(result.getThrowable());
    } else {
      success(result.getValue());
    }
  }

}
