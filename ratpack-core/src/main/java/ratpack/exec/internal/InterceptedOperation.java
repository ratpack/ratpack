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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.ExecInterceptor;
import ratpack.util.ExceptionUtils;

import java.util.List;

public abstract class InterceptedOperation {
  private final static Logger LOGGER = LoggerFactory.getLogger(InterceptedOperation.class);

  private final ExecInterceptor.ExecType type;
  private final List<ExecInterceptor> interceptors;
  private Throwable thrown;

  private int i;

  public InterceptedOperation(ExecInterceptor.ExecType type, List<ExecInterceptor> interceptors) {
    this.type = type;
    this.interceptors = interceptors;
    i = 0;
  }

  public void run() throws Exception {
    if (interceptors.isEmpty()) {
      performOperation();
    } else {
      nextInterceptor();
      if (thrown != null) {
        throw ExceptionUtils.toException(thrown);
      }
    }
  }

  private void nextInterceptor() {
    if (i < interceptors.size()) {
      int iAtStart = i;
      ExecInterceptor interceptor = interceptors.get(i);
      Runnable continuation = () -> {
        try {
          ++i;
          nextInterceptor();
        } catch (Exception ignore) {
          // do nothing
        }
      };
      try {
        interceptor.intercept(type, continuation);
      } catch (Throwable e) {
        LOGGER.error("", e);
        if (i == iAtStart) {
          continuation.run();
        }
      }
    } else {
      try {
        performOperation();
      } catch (Throwable e) {
        thrown = e;
      }
    }
  }

  abstract protected void performOperation() throws Exception;

}
