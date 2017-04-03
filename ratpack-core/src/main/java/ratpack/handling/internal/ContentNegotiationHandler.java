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

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.func.Action;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.internal.MimeParse;
import ratpack.registry.Registry;

import java.util.Map;

public class ContentNegotiationHandler implements Handler {

  private final Map<String, Handler> handlers;
  private final Handler noMatchHandler;
  private final Handler unspecifiedHandler;

  public ContentNegotiationHandler(Registry registry, Action<? super ByContentSpec> action) throws Exception {
    Map<String, Handler> handlers = Maps.newLinkedHashMap();
    this.handlers = handlers;

    DefaultByContentSpec spec = new DefaultByContentSpec(registry, handlers);
    action.execute(spec);

    this.noMatchHandler = spec.noMatchHandler;
    this.unspecifiedHandler = spec.unspecifiedHandler;
  }

  @Override
  public void handle(Context context) throws Exception {
    if (handlers.isEmpty()) {
      noMatchHandler.handle(context);
      return;
    }

    String acceptHeader = context.getRequest().getHeaders().get(HttpHeaderNames.ACCEPT);
    if (Strings.isNullOrEmpty(acceptHeader)) {
      unspecifiedHandler.handle(context);
    } else {
      String winner = MimeParse.bestMatch(handlers.keySet(), acceptHeader);
      if (Strings.isNullOrEmpty(winner)) {
        noMatchHandler.handle(context);
      } else {
        context.getResponse().contentType(winner);
        handlers.get(winner).handle(context);
      }
    }
  }

  public static class DefaultByContentSpec implements ByContentSpec {

    private static final String TYPE_PLAIN_TEXT = "text/plain";
    private static final String TYPE_HTML = "text/html";
    private static final String TYPE_JSON = "application/json";
    private static final String TYPE_XML = "application/xml";

    private final Map<String, Handler> handlers;
    private final Registry registry;

    private Handler noMatchHandler = ctx -> ctx.clientError(406);
    private Handler unspecifiedHandler;

    public DefaultByContentSpec(Registry registry, Map<String, Handler> handlers) {
      this.registry = registry;
      this.handlers = handlers;
      this.unspecifiedHandler = ctx -> {
        Map.Entry<String, Handler> first = Iterables.getFirst(handlers.entrySet(), null);
        if (first == null) {
          noMatchHandler.handle(ctx);
        } else {
          ctx.getResponse().contentType(first.getKey());
          first.getValue().handle(ctx);
        }
      };
    }

    @Override
    public ByContentSpec type(String mimeType, Handler handler) {
      if (mimeType == null) {
        throw new IllegalArgumentException("mimeType cannot be null");
      }

      String trimmed = mimeType.trim();
      if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("mimeType cannot be a blank string");
      }

      if (trimmed.contains("*")) {
        throw new IllegalArgumentException("mimeType cannot include wildcards");
      }

      handlers.put(mimeType, handler);
      return this;
    }

    @Override
    public ByContentSpec plainText(Handler handler) {
      return type(TYPE_PLAIN_TEXT, handler);
    }

    @Override
    public ByContentSpec html(Handler handler) {
      return type(TYPE_HTML, handler);
    }

    @Override
    public ByContentSpec json(Handler handler) {
      return type(TYPE_JSON, handler);
    }

    @Override
    public ByContentSpec xml(Handler handler) {
      return type(TYPE_XML, handler);
    }

    @Override
    public ByContentSpec noMatch(Handler handler) {
      noMatchHandler = handler;
      return this;
    }

    @Override
    public ByContentSpec noMatch(String mimeType) {
      noMatchHandler = handleWithMimeTypeBlock(mimeType);
      return this;
    }

    @Override
    public ByContentSpec unspecified(Handler handler) {
      unspecifiedHandler = handler;
      return this;
    }

    @Override
    public ByContentSpec unspecified(String mimeType) {
      unspecifiedHandler = handleWithMimeTypeBlock(mimeType);
      return this;
    }

    private Handler handleWithMimeTypeBlock(String mimeType) {
      return ctx -> {
        Handler handler = handlers.get(mimeType);
        if (handler == null) {
          ctx.error(new IllegalStateException("No handler defined for mimeType " + mimeType));
        } else {
          ctx.getResponse().contentType(mimeType);
          handler.handle(ctx);
        }
      };
    }

    @Override
    public ByContentSpec type(String mimeType, Class<? extends Handler> handlerType) {
      return type(mimeType, registry.get(handlerType));
    }

    @Override
    public ByContentSpec plainText(Class<? extends Handler> handlerType) {
      return plainText(registry.get(handlerType));
    }

    @Override
    public ByContentSpec html(Class<? extends Handler> handlerType) {
      return html(registry.get(handlerType));
    }

    @Override
    public ByContentSpec json(Class<? extends Handler> handlerType) {
      return json(registry.get(handlerType));
    }

    @Override
    public ByContentSpec xml(Class<? extends Handler> handlerType) {
      return xml(registry.get(handlerType));
    }

    @Override
    public ByContentSpec noMatch(Class<? extends Handler> handlerType) {
      return noMatch(registry.get(handlerType));
    }

    @Override
    public ByContentSpec unspecified(Class<? extends Handler> handlerType) {
      return unspecified(registry.get(handlerType));
    }
  }
}
