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

import ratpack.func.NoArgAction;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Handler;

import java.util.Map;

public class DefaultByContentSpec implements ByContentSpec {

  public static final String TYPE_PLAIN_TEXT = "text/plain";
  public static final String TYPE_HTML = "text/html";
  public static final String TYPE_JSON = "application/json";
  public static final String TYPE_XML = "application/xml";

  private final Map<String, Handler> map;
  private Handler noMatchHandler = ctx -> ctx.clientError(406);

  public DefaultByContentSpec(Map<String, Handler> map) {
    this.map = map;
  }

  @Override
  public ByContentSpec type(String mimeType, NoArgAction handler) {
    return type(mimeType, ctx -> handler.execute());
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

    map.put(trimmed, handler);
    return this;
  }

  @Override
  public ByContentSpec plainText(NoArgAction handler) {
    return type(TYPE_PLAIN_TEXT, handler);
  }

  @Override
  public ByContentSpec plainText(Handler handler) {
    return type(TYPE_PLAIN_TEXT, handler);
  }

  @Override
  public ByContentSpec html(NoArgAction handler) {
    return type(TYPE_HTML, handler);
  }

  @Override
  public ByContentSpec html(Handler handler) {
    return type(TYPE_HTML, handler);
  }

  @Override
  public ByContentSpec json(NoArgAction handler) {
    return type(TYPE_JSON, handler);
  }

  @Override
  public ByContentSpec json(Handler handler) {
    return type(TYPE_JSON, handler);
  }

  @Override
  public ByContentSpec xml(NoArgAction handler) {
    return type(TYPE_XML, handler);
  }

  @Override
  public ByContentSpec xml(Handler handler) {
    return type(TYPE_XML, handler);
  }

  @Override
  public ByContentSpec noMatch(NoArgAction handler) {
    noMatchHandler = ctx -> handler.execute();
    return this;
  }

  @Override
  public ByContentSpec noMatch(String mimeType) {
    return noMatch(ctx -> {
      ctx.getResponse().contentType(mimeType);
      ctx.insert(map.get(mimeType));
    });
  }

  @Override
  public ByContentSpec noMatch(Handler handler) {
    noMatchHandler = handler;
    return this;
  }

  public Handler getNoMatchHandler() {
    return noMatchHandler;
  }
}
