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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.func.Action;
import ratpack.func.NoArgAction;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContentNegotiationHandler implements Handler {

  private final ImmutableMap<String, NoArgAction> handlers;
  private final Action<Context> noMatchHandler;

  public ContentNegotiationHandler(Map<String, NoArgAction> handlers, Action<Context> noMatchHandler) {
    this.handlers = ImmutableMap.copyOf(handlers);
    this.noMatchHandler = noMatchHandler;
  }

  @Override
  public void handle(Context context) throws Exception {
    if (handlers.isEmpty()) {
      noMatchHandler.execute(context);
      return;
    }

    List<String> types = Lists.newArrayList(handlers.keySet());
    String winner = types.get(0);
    Collections.reverse(types);

    String acceptHeader = context.getRequest().getHeaders().get(HttpHeaderNames.ACCEPT);
    if (!Strings.isNullOrEmpty(acceptHeader)) {
      winner = MimeParse.bestMatch(types, acceptHeader);
    }

    if (Strings.isNullOrEmpty(winner)) {
      noMatchHandler.execute(context);
    } else {
      context.getResponse().contentType(winner);
      handlers.get(winner).execute();
    }
  }
}
