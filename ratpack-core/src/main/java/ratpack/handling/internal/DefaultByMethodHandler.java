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

import com.google.common.base.Joiner;
import ratpack.handling.ByMethodHandler;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

public class DefaultByMethodHandler implements ByMethodHandler {

  private final Map<String, Runnable> runnables = new LinkedHashMap<>(2);

  public ratpack.handling.ByMethodHandler get(Runnable runnable) {
    return named("GET", runnable);
  }

  public ratpack.handling.ByMethodHandler post(Runnable runnable) {
    return named("POST", runnable);
  }

  public ratpack.handling.ByMethodHandler put(Runnable runnable) {
    return named("PUT", runnable);
  }

  public ratpack.handling.ByMethodHandler delete(Runnable runnable) {
    return named("DELETE", runnable);
  }

  public ratpack.handling.ByMethodHandler named(String methodName, Runnable runnable) {
    runnables.put(methodName.toUpperCase(), runnable);
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
    if (context.getRequest().getMethod().isOptions()) {
      String methods = Joiner.on(",").join(runnables.keySet());
      context.getResponse().getHeaders().add("Allow", methods);
      context.getResponse().status(200).send();
    } else {
      Handler[] handlers = new Handler[runnables.size() + 1];
      int i = 0;
      for (Map.Entry<String, Runnable> entry : runnables.entrySet()) {
        handlers[i++] = new ByMethodHandler(entry.getKey(), entry.getValue());
      }
      handlers[i] = new ClientErrorForwardingHandler(METHOD_NOT_ALLOWED.code());
      context.insert(handlers);
    }
  }

}
