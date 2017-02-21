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
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Map;

public class DefaultByMethodSpec implements ByMethodSpec {

  public static final String METHOD_GET = "GET";
  public static final String METHOD_POST = "POST";
  public static final String METHOD_PUT = "PUT";
  public static final String METHOD_PATCH = "PATCH";
  public static final String METHOD_OPTIONS = "OPTIONS";
  public static final String METHOD_DELETE = "DELETE";

  private final Map<String, Block> blocks;
  private final Context context;

  public DefaultByMethodSpec(Map<String, Block> blocks, Context context) {
    this.blocks = blocks;
    this.context = context;
  }

  @Override
  public ByMethodSpec get(Block block) {
    return named(METHOD_GET, block);
  }

  @Override
  public ByMethodSpec get(Class<? extends Handler> clazz) {
    return get(block(clazz));
  }

  @Override
  public ByMethodSpec get(Handler handler) {
    return get(block(handler));
  }

  @Override
  public ByMethodSpec post(Block block) {
    return named(METHOD_POST, block);
  }

  @Override
  public ByMethodSpec post(Class<? extends Handler> clazz) {
    return post(block(clazz));
  }

  @Override
  public ByMethodSpec post(Handler handler) {
    return post(block(handler));
  }

  @Override
  public ByMethodSpec put(Block block) {
    return named(METHOD_PUT, block);
  }

  @Override
  public ByMethodSpec put(Class<? extends Handler> clazz) {
    return put(block(clazz));
  }

  @Override
  public ByMethodSpec put(Handler handler) {
    return put(block(handler));
  }

  @Override
  public ByMethodSpec patch(Block block) {
    return named(METHOD_PATCH, block);
  }

  @Override
  public ByMethodSpec patch(Class<? extends Handler> clazz) {
    return patch(block(clazz));
  }

  @Override
  public ByMethodSpec patch(Handler handler) {
    return patch(block(handler));
  }

  @Override
  public ByMethodSpec options(Block block) {
    return named(METHOD_OPTIONS, block);
  }

  @Override
  public ByMethodSpec options(Class<? extends Handler> clazz) {
    return options(block(clazz));
  }

  @Override
  public ByMethodSpec options(Handler handler) {
    return options(block(handler));
  }

  @Override
  public ByMethodSpec delete(Block block) {
    return named(METHOD_DELETE, block);
  }

  @Override
  public ByMethodSpec delete(Class<? extends Handler> clazz) {
    return delete(block(clazz));
  }

  @Override
  public ByMethodSpec delete(Handler handler) {
    return delete(block(handler));
  }

  @Override
  public ByMethodSpec named(String methodName, Block block) {
    blocks.put(methodName, block);
    return this;
  }

  @Override
  public ByMethodSpec named(String methodName, Class<? extends Handler> clazz) {
    return named(methodName, block(clazz));
  }

  @Override
  public ByMethodSpec named(String methodName, Handler handler) {
    return named(methodName, block(handler));
  }

  private Block block(Class<? extends Handler> clazz) {
    return block(context.get(clazz));
  }

  private Block block(Handler handler) {
    return () -> context.insert(handler);
  }
}
