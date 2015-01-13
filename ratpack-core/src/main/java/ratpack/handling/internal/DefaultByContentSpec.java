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
import ratpack.http.internal.MimeParse;

import java.util.Map;

public class DefaultByContentSpec implements ByContentSpec {

  private final Map<String, NoArgAction> map;

  public DefaultByContentSpec(Map<String, NoArgAction> map) {
    this.map = map;
  }

  public ByContentSpec type(String mimeType, NoArgAction handler) {
    if (mimeType == null) {
      throw new IllegalArgumentException("mimeType cannot be null");
    }

    String trimmed = mimeType.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("mimeType cannot be a blank string");
    }

    map.put(trimmed, handler);
    return this;
  }

  public ByContentSpec plainText(NoArgAction handler) {
    return type("text/plain", handler);
  }

  public ByContentSpec html(NoArgAction handler) {
    return type("text/html", handler);
  }

  public ByContentSpec json(NoArgAction handler) {
    return type("application/json", handler);
  }

  public ByContentSpec xml(NoArgAction handler) {
    return type("application/xml", handler);
  }

  @Override
  public ByContentSpec allTypes(NoArgAction handler) {
    return type(MimeParse.MIME_ANY, handler);
  }

}
