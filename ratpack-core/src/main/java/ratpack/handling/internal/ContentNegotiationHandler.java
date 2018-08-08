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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.internal.MimeParse;
import ratpack.registry.Registry;

import java.util.*;

public class ContentNegotiationHandler implements Handler {

  private final Map<String, Handler> handlers = new LinkedHashMap<>();
  private final List<String> reverseKeys;
  private final Handler noMatchHandler;
  private final Handler unspecifiedHandler;
  private final Function<String, String> mimeTypeDecorator;

  public ContentNegotiationHandler(Registry registry, Action<? super ByContentSpec> action) throws Exception {
    DefaultByContentSpec spec = new DefaultByContentSpec(registry, handlers);
    action.execute(spec);

    this.noMatchHandler = spec.noMatchHandler;
    this.unspecifiedHandler = spec.unspecifiedHandler;
    this.mimeTypeDecorator = spec.mimeTypeDecorator;

    this.reverseKeys = new ArrayList<>(handlers.keySet());
    Collections.reverse(reverseKeys);
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
      String winner = MimeParse.bestMatch(reverseKeys, acceptHeader);
      if (Strings.isNullOrEmpty(winner)) {
        noMatchHandler.handle(context);
      } else {
        String decoratedMimeType = mimeTypeDecorator.apply(winner);
        context.getResponse().contentType(decoratedMimeType);
        handlers.get(winner).handle(context);
      }
    }
  }

  public static class DefaultByContentSpec implements ByContentSpec {

    private static final String TYPE_HTML = "text/html";
    private static final String TYPE_TEXT = "text/plain";
    private static final String TYPE_XML = "application/xml";
    private static final String UTF8_CHARSET_SUFFIX = ";charset=UTF-8";

    private final Map<String, Handler> handlers;
    private final Registry registry;

    private Handler noMatchHandler = ctx -> ctx.clientError(406);
    private Handler unspecifiedHandler;
    private final Function<String, String> mimeTypeDecorator;

    public DefaultByContentSpec(Registry registry, Map<String, Handler> handlers) {
      this.registry = registry;
      this.handlers = handlers;
      this.mimeTypeDecorator = mimeType -> {
        switch (mimeType) {
          case TYPE_HTML:
            return TYPE_HTML + UTF8_CHARSET_SUFFIX;
          case TYPE_TEXT:
            return TYPE_TEXT + UTF8_CHARSET_SUFFIX;
          default:
            return mimeType;
        }
      };
      this.unspecifiedHandler = ctx -> {
        Map.Entry<String, Handler> first = Iterables.getFirst(handlers.entrySet(), null);
        if (first == null) {
          noMatchHandler.handle(ctx);
        } else {
          String decoratedMimeType = mimeTypeDecorator.apply(first.getKey());
          ctx.getResponse().contentType(decoratedMimeType);
          first.getValue().handle(ctx);
        }
      };
    }

    @Override
    public ByContentSpec type(String mimeType, Handler handler) {
      return type((CharSequence) mimeType, handler);
    }

    @Override
    public ByContentSpec type(CharSequence mimeType, Handler handler) {
      if (mimeType == null) {
        throw new IllegalArgumentException("mimeType cannot be null");
      }

      String trimmed = mimeType.toString().trim();
      if (trimmed.isEmpty()) {
        throw new IllegalArgumentException("mimeType cannot be a blank string");
      }

      if (trimmed.contains("*")) {
        throw new IllegalArgumentException("mimeType cannot include wildcards");
      }

      handlers.put(trimmed, handler);
      return this;
    }

    @Override
    public ByContentSpec plainText(Handler handler) {
      return type(TYPE_TEXT, handler);
    }

    @Override
    public ByContentSpec html(Handler handler) {
      return type(TYPE_HTML, handler);
    }

    @Override
    public ByContentSpec json(Handler handler) {
      return type(HttpHeaderValues.APPLICATION_JSON, handler);
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
          String decoratedMimeType = mimeTypeDecorator.apply(mimeType);
          ctx.getResponse().contentType(decoratedMimeType);
          handler.handle(ctx);
        }
      };
    }

    @Override
    public ByContentSpec type(String mimeType, Class<? extends Handler> handlerType) {
      return type(mimeType, registry.get(handlerType));
    }

    @Override
    public ByContentSpec type(CharSequence mimeType, Class<? extends Handler> handlerType) {
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
