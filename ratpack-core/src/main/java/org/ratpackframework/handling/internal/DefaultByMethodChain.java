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

import org.ratpackframework.handling.ByMethodChain;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

public class DefaultByMethodChain implements ByMethodChain {

  private final Exchange exchange;
  private final Map<String, Runnable> runnables = new LinkedHashMap<String, Runnable>(2);

  public DefaultByMethodChain(Exchange exchange) {
    this.exchange = exchange;
  }

  public ByMethodChain get(Runnable runnable) {
    return named("get", runnable);
  }

  public ByMethodChain post(Runnable runnable) {
    return named("post", runnable);
  }

  public ByMethodChain named(String methodName, Runnable runnable) {
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

    public void handle(Exchange exchange) {
      if (exchange.getRequest().getMethod().name(method)) {
        runnable.run();
      } else {
        exchange.next();
      }
    }
  }

  public void call() {
    List<Handler> handlers = new ArrayList<Handler>(runnables.size());
    for (Map.Entry<String, Runnable> entry : runnables.entrySet()) {
      handlers.add(new ByMethodHandler(entry.getKey(), entry.getValue()));
    }
    handlers.add(new ClientErrorHandler(METHOD_NOT_ALLOWED.code()));
    exchange.next(handlers);
  }

}
