/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.error.internal;

import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.HandlerException;

public class ErrorCatchingHandler implements Handler {

  private final Handler handler;

  public ErrorCatchingHandler(Handler handler) {
    this.handler = handler;
  }

  public void handle(Context context) {
    try {
      handler.handle(context);
    } catch (Exception exception) {
      if (exception instanceof HandlerException) {
        ((HandlerException) exception).getContext().error((Exception) exception.getCause());
      } else {
        context.error(exception);
      }
    }
  }
}
