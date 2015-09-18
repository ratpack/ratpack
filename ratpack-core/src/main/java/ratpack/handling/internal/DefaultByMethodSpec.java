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
import ratpack.handling.ByMethodSpec;

import java.util.Map;

public class DefaultByMethodSpec implements ByMethodSpec {

  public static final String METHOD_GET = "GET";
  public static final String METHOD_POST = "POST";
  public static final String METHOD_PUT = "PUT";
  public static final String METHOD_PATCH = "PATCH";
  public static final String METHOD_OPTIONS = "OPTIONS";
  public static final String METHOD_DELETE = "DELETE";

  private final Map<String, Block> blocks;

  public DefaultByMethodSpec(Map<String, Block> blocks) {
    this.blocks = blocks;
  }

  @Override
  public ByMethodSpec get(Block block) {
    return named(METHOD_GET, block);
  }

  @Override
  public ByMethodSpec post(Block block) {
    return named(METHOD_POST, block);
  }

  @Override
  public ByMethodSpec put(Block block) {
    return named(METHOD_PUT, block);
  }

  @Override
  public ByMethodSpec patch(Block block) {
    return named(METHOD_PATCH, block);
  }

  @Override
  public ByMethodSpec options(Block block) {
    return named(METHOD_OPTIONS, block);
  }

  @Override
  public ByMethodSpec delete(Block block) {
    return named(METHOD_DELETE, block);
  }

  @Override
  public ByMethodSpec named(String methodName, Block block) {
    blocks.put(methodName, block);
    return this;
  }

}
