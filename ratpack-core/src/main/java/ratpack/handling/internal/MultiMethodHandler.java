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
import com.google.common.collect.Maps;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.handling.ByMethodSpec;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.http.HttpMethod;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.registry.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

public class MultiMethodHandler implements Handler {

  private static final Joiner JOINER = Joiner.on(",");
  private static final Handler NO_METHOD_HANDLER = Handlers.clientError(METHOD_NOT_ALLOWED.code());
  private final Map<HttpMethod, Handler> handlers;

  public MultiMethodHandler(Registry registry, Action<? super ByMethodSpec> action) throws Exception {
    this.handlers = Maps.newHashMap();
    action.execute(new DefaultByMethodSpec(registry, handlers));
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
      Handler handler = handlers.get(method);
      if (handler != null) {
        context.insert(handler);
        return;
      }

      if (method.isHead()) {
        Handler getHandler = handlers.get(HttpMethod.GET);
        if (getHandler != null) {
          context.insert(getHandler);
          return;
        }
      }

      NO_METHOD_HANDLER.handle(context);
    }
  }

  public static class DefaultByMethodSpec implements ByMethodSpec {

    private final Map<HttpMethod, Handler> handlers;
    private final Registry registry;

    public DefaultByMethodSpec(Registry registry, Map<HttpMethod, Handler> handlers) {
      this.handlers = handlers;
      this.registry = registry;
    }

    @Override
    public ByMethodSpec get(Block block) {
      return get(handler(block));
    }

    @Override
    public ByMethodSpec get(Class<? extends Handler> clazz) {
      return get(handler(clazz));
    }

    @Override
    public ByMethodSpec get(Handler handler) {
      return add(HttpMethod.GET, handler);
    }

    @Override
    public ByMethodSpec post(Block block) {
      return post(handler(block));
    }

    @Override
    public ByMethodSpec post(Class<? extends Handler> clazz) {
      return post(handler(clazz));
    }

    @Override
    public ByMethodSpec post(Handler handler) {
      return add(HttpMethod.POST, handler);
    }

    @Override
    public ByMethodSpec put(Block block) {
      return put(handler(block));
    }

    @Override
    public ByMethodSpec put(Class<? extends Handler> clazz) {
      return put(handler(clazz));
    }

    @Override
    public ByMethodSpec put(Handler handler) {
      return add(HttpMethod.PUT, handler);
    }

    @Override
    public ByMethodSpec patch(Block block) {
      return patch(handler(block));
    }

    @Override
    public ByMethodSpec patch(Class<? extends Handler> clazz) {
      return patch(handler(clazz));
    }

    @Override
    public ByMethodSpec patch(Handler handler) {
      return add(HttpMethod.PATCH, handler);
    }

    @Override
    public ByMethodSpec options(Block block) {
      return options(handler(block));
    }

    @Override
    public ByMethodSpec options(Class<? extends Handler> clazz) {
      return options(handler(clazz));
    }

    @Override
    public ByMethodSpec options(Handler handler) {
      return add(HttpMethod.OPTIONS, handler);
    }

    @Override
    public ByMethodSpec delete(Block block) {
      return delete(handler(block));
    }

    @Override
    public ByMethodSpec delete(Class<? extends Handler> clazz) {
      return delete(handler(clazz));
    }

    @Override
    public ByMethodSpec delete(Handler handler) {
      return add(HttpMethod.DELETE, handler);
    }

    @Override
    public ByMethodSpec named(String methodName, Block block) {
      named(methodName, handler(block));
      return this;
    }

    @Override
    public ByMethodSpec named(String methodName, Class<? extends Handler> clazz) {
      return named(methodName, handler(clazz));
    }

    @Override
    public ByMethodSpec named(String methodName, Handler handler) {
      return add(HttpMethod.of(methodName), handler);
    }

    private ByMethodSpec add(HttpMethod method, Handler handler) {
      handlers.put(method, handler);
      return this;
    }

    private Handler handler(Class<? extends Handler> clazz) {
      return registry.get(clazz);
    }

    private Handler handler(Block block) {
      return ctx -> block.execute();
    }

  }
}
