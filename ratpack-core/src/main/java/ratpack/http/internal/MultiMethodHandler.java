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

package ratpack.http.internal;

import com.google.common.base.Joiner;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.http.HttpMethod;

import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

public class MultiMethodHandler implements Handler {

  private static final Handler NO_METHOD_HANDLER = Handlers.clientError(METHOD_NOT_ALLOWED.code());

  private final Map<String, Handler> handlers;

  public MultiMethodHandler(Map<String, Handler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void handle(Context context) throws Exception {
    HttpMethod method = context.getRequest().getMethod();
    if (method.isOptions()) {
      String methods = Joiner.on(",").join(handlers.keySet());
      context.getResponse().getHeaders().add(HttpHeaderConstants.ALLOW, methods);
      context.getResponse().status(200).send();
    } else {
      for (Map.Entry<String, Handler> entry : handlers.entrySet()) {
        String key = entry.getKey();
        if (method.name(key)) {
          entry.getValue().handle(context);
          return;
        }
      }

      NO_METHOD_HANDLER.handle(context);
    }
  }

}
