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

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.http.HttpMethod;
import ratpack.http.internal.HttpHeaderConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

public class MultiMethodHandler implements Handler {

  private static final Joiner JOINER = Joiner.on(",");
  private static final Handler NO_METHOD_HANDLER = Handlers.clientError(METHOD_NOT_ALLOWED.code());
  private final Map<HttpMethod, Handler> handlers;

  public MultiMethodHandler(Map<HttpMethod, Handler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void handle(Context context) throws Exception {
    HttpMethod method = context.getRequest().getMethod();
    if (method.isOptions() && !handlers.containsKey(HttpMethod.OPTIONS)) {
      List<String> parts = new ArrayList<>(Collections2.transform(handlers.keySet(), HttpMethod::getName));
      Collections.sort(parts);
      String methods = JOINER.join(parts);
      context.getResponse().getHeaders().add(HttpHeaderConstants.ALLOW, methods);
      context.getResponse().status(200).send();
    } else {
      for (Map.Entry<HttpMethod, Handler> entry : handlers.entrySet()) {
        HttpMethod key = entry.getKey();
        if (key.equals(method)) {
          context.insert(entry.getValue());
          return;
        }
      }

      NO_METHOD_HANDLER.handle(context);
    }
  }

}
