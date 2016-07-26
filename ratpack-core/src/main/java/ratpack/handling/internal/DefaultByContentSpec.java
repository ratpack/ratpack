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

import ratpack.func.Block;
import ratpack.handling.ByContentSpec;
import ratpack.handling.Handler;

import java.util.Map;

public class DefaultByContentSpec implements ByContentSpec {

  public static final String TYPE_PLAIN_TEXT = "text/plain";
  public static final String TYPE_HTML = "text/html";
  public static final String TYPE_JSON = "application/json";
  public static final String TYPE_XML = "application/xml";

  private final Map<String, Block> blocks;
  private Handler noMatchHandler = ctx -> ctx.clientError(406);
  private Handler unspecifiedHandler;

  public DefaultByContentSpec(Map<String, Block> blocks) {
    this.blocks = blocks;
    this.unspecifiedHandler = ctx -> {
      String winner = blocks.keySet().stream().findFirst().orElseThrow(IllegalStateException::new);
      ctx.getResponse().contentType(winner);
      blocks.get(winner).execute();
    };
  }

  @Override
  public ByContentSpec type(String mimeType, Block block) {
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

    blocks.put(mimeType, block);
    return this;
  }

  @Override
  public ByContentSpec plainText(Block handler) {
    return type(TYPE_PLAIN_TEXT, handler);
  }

  @Override
  public ByContentSpec html(Block handler) {
    return type(TYPE_HTML, handler);
  }

  @Override
  public ByContentSpec json(Block handler) {
    return type(TYPE_JSON, handler);
  }

  @Override
  public ByContentSpec xml(Block handler) {
    return type(TYPE_XML, handler);
  }

  @Override
  public ByContentSpec noMatch(Block handler) {
    noMatchHandler = ctx -> handler.execute();
    return this;
  }

  @Override
  public ByContentSpec noMatch(String mimeType) {
    noMatchHandler = handleWithMimeTypeBlock(mimeType);
    return this;
  }

  @Override
  public ByContentSpec unspecified(Block handler) {
    unspecifiedHandler = ctx -> handler.execute();
    return this;
  }

  @Override
  public ByContentSpec unspecified(String mimeType) {
    unspecifiedHandler = handleWithMimeTypeBlock(mimeType);
    return this;
  }

  public Handler getNoMatchHandler() {
    return noMatchHandler;
  }

  public Handler getUnspecifiedHandler() {
    return unspecifiedHandler;
  }

  private Handler handleWithMimeTypeBlock(String mimeType) {
    return (ctx) -> {
      Block block = blocks.get(mimeType);
      if (block == null) {
        ctx.error(new IllegalStateException("No block defined for mimeType " + mimeType));
      } else {
        ctx.getResponse().contentType(mimeType);
        block.execute();
      }
    };
  }
}
