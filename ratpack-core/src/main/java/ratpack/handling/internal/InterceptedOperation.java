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

package ratpack.handling.internal;

import ratpack.handling.Context;
import ratpack.handling.ProcessingInterceptor;

import java.util.List;

public abstract class InterceptedOperation {
  private final ProcessingInterceptor.Type type;
  private final List<ProcessingInterceptor> interceptors;
  private final Context context;

  public InterceptedOperation(ProcessingInterceptor.Type type, List<ProcessingInterceptor> interceptors, Context context) {
    this.type = type;
    this.interceptors = interceptors;
    this.context = context;
  }

  public void run() {
    if (interceptors.isEmpty()) {
      performOperation();
    } else {
      nextInterceptor(interceptors.size() - 1);
    }
  }

  private void nextInterceptor(final int i) {
    if (i >= 0) {
      ProcessingInterceptor interceptor = interceptors.get(i);
      Runnable continuation = new Runnable() {
        @Override
        public void run() {
          try {
            nextInterceptor(i - 1);
          } catch (Exception ignore) {
            // do nothing
          }
        }
      };
      try {
        interceptor.intercept(type, context, continuation);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    } else {
      performOperation();
    }
  }

  abstract protected void performOperation();

}
