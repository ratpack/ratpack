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

package ratpack.handling.internal;

import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.List;

public class ChainHandler implements Handler {

  private final Handler[] handlers;

  public ChainHandler(List<? extends Handler> handlers) {
    this.handlers = handlers.toArray(new Handler[handlers.size()]);
  }

  public ChainHandler(Handler... handlers) {
    this.handlers = handlers;
  }

  public static Handler[] unpack(Handler handler) {
    if (handler instanceof ChainHandler) {
      return ((ChainHandler) handler).handlers;
    } else {
      return new Handler[]{handler};
    }
  }

  public void handle(Context context) {
    context.insert(handlers);
  }

  public Handler[] getHandlers() {
    return handlers;
  }
}
