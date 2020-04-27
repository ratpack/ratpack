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

package ratpack.exec.util.internal;

import ratpack.api.UncheckedException;
import ratpack.exec.ExecResult;
import ratpack.exec.Promise;
import ratpack.util.Exceptions;

import java.util.List;

public abstract class AbstractPromiseZipper {

  protected final List<Promise<?>> promises;

  public AbstractPromiseZipper(List<Promise<?>> promises) {
    this.promises = promises;
  }


  protected void throwIfError(ExecResult<?>... execResult) {
    for (int i = 0; i < execResult.length; i++) {
      if (execResult[i].getThrowable() != null) {
        throw Exceptions.uncheck(execResult[i].getThrowable());
      }
    }
  }

  protected <R> Promise<R> unpack(Throwable t) {
    if (t instanceof UncheckedException) {
      return Promise.error(t.getCause());
    }
    return Promise.error(t);
  }
}
