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

package org.ratpackframework.handling.internal;

import org.ratpackframework.handling.ByMethodHandler;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

public class DefaultByMethodHandler implements ByMethodHandler {

  private final Map<String, Runnable> runnables = new LinkedHashMap<>(2);

  public org.ratpackframework.handling.ByMethodHandler get(Runnable runnable) {
    return named("get", runnable);
  }

  public org.ratpackframework.handling.ByMethodHandler post(Runnable runnable) {
    return named("post", runnable);
  }

  public org.ratpackframework.handling.ByMethodHandler put(Runnable runnable) {
    return named("put", runnable);
  }

  public org.ratpackframework.handling.ByMethodHandler delete(Runnable runnable) {
    return named("delete", runnable);
  }

  public org.ratpackframework.handling.ByMethodHandler named(String methodName, Runnable runnable) {
    runnables.put(methodName.toLowerCase(), runnable);
    return this;
  }

  private static class ByMethodHandler implements Handler {
    private final Runnable runnable;
    private final String method;

    private ByMethodHandler(String method, Runnable runnable) {
      this.method = method;
      this.runnable = runnable;
    }

    public void handle(Context context) {
      if (context.getRequest().getMethod().name(method)) {
        runnable.run();
      } else {
        context.next();
      }
    }
  }

  public void handle(Context context) {
    List<Handler> handlers = new ArrayList<>(runnables.size() + 1);
    for (Map.Entry<String, Runnable> entry : runnables.entrySet()) {
      handlers.add(new ByMethodHandler(entry.getKey(), entry.getValue()));
    }
    handlers.add(new ClientErrorHandler(METHOD_NOT_ALLOWED.code()));
    context.insert(handlers);
  }

}
