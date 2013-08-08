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

package org.ratpackframework.handling.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.error.ClientErrorHandler;
import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.*;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.redirect.Redirector;
import org.ratpackframework.registry.NotInRegistryException;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.ObjectHoldingChildRegistry;
import org.ratpackframework.render.NoSuchRendererException;
import org.ratpackframework.render.RenderController;

import java.io.File;
import java.util.List;
import java.util.Map;


public class DefaultContext implements Context {

  private final Request request;
  private final Response response;

  private final ChannelHandlerContext channelHandlerContext;

  private final Handler next;
  private final Registry<Object> registry;

  public DefaultContext(Request request, Response response, ChannelHandlerContext channelHandlerContext, Registry<Object> registry, Handler next) {
    this.request = request;
    this.response = response;
    this.channelHandlerContext = channelHandlerContext;
    this.registry = registry;
    this.next = next;
  }

  public Request getRequest() {
    return request;
  }

  public Response getResponse() {
    return response;
  }

  public <O> O get(Class<O> type) throws NotInRegistryException {
    return registry.get(type);
  }

  public <O> O maybeGet(Class<O> type) {
    return registry.maybeGet(type);
  }

  public void next() {
    next.handle(this);
  }

  public void insert(List<Handler> handlers) {
    doNext(this, registry, handlers, 0, next);
  }

  public void insert(Registry<Object> registry, List<Handler> handlers) {
    doNext(this, registry, handlers, 0, next);
  }

  public <P, T extends P> void insert(Class<P> publicType, T implementation, List<Handler> handlers) {
    doNext(this, new ObjectHoldingChildRegistry<Object>(registry, publicType, implementation), handlers, 0, next);
  }

  public void insert(Object object, List<Handler> handlers) {
    doNext(this, new ObjectHoldingChildRegistry<Object>(registry, object), handlers, 0, next);
  }

  public void respond(Responder responder) {
    responder.respond(this);
  }

  public Map<String, String> getPathTokens() {
    return get(PathBinding.class).getTokens();
  }

  public Map<String, String> getAllPathTokens() {
    return get(PathBinding.class).getAllTokens();
  }

  public File file(String path) {
    return get(FileSystemBinding.class).file(path);
  }

  public void render(Object object) throws NoSuchRendererException {
    get(RenderController.class).render(this, object);
  }

  public void redirect(String location) throws NotInRegistryException {
    redirect(HttpResponseStatus.FOUND.code(), location);
  }

  public void redirect(int code, String location) throws NotInRegistryException {
    Redirector redirector = registry.get(Redirector.class);
    redirector.redirect(this, response, request, location, code);
  }

  public void error(Exception exception) {
    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.class);
    serverErrorHandler.error(this, exception);
  }

  public void clientError(int statusCode) {
    get(ClientErrorHandler.class).error(this, statusCode);
  }

  public void withErrorHandling(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      if (e instanceof HandlerException) {
        ((HandlerException) e).getContext().error((Exception) e.getCause());
      } else {
        error(e);
      }
    }
  }

  public ByMethodResponder getByMethod() {
    return new DefaultByMethodResponder();
  }

  public ByContentResponder getByContent() {
    return new DefaultByContentResponder();
  }

  protected void doNext(final Context parentContext, final Registry<Object> registry, final List<Handler> handlers, final int index, final Handler exhausted) {
    assert registry != null;
    if (index == handlers.size()) {
      try {
        exhausted.handle(parentContext);
      } catch (Exception e) {
        if (e instanceof HandlerException) {
          throw (HandlerException) e;
        } else {
          throw new HandlerException(this, e);
        }
      }

    } else {
      Handler handler = handlers.get(index);
      Handler nextHandler = new Handler() {
        public void handle(Context exchange) {
          ((DefaultContext) exchange).doNext(parentContext, registry, handlers, index + 1, exhausted);
        }
      };
      DefaultContext childExchange = new DefaultContext(request, response, channelHandlerContext, registry, nextHandler);
      try {
        handler.handle(childExchange);
      } catch (Exception e) {
        if (e instanceof HandlerException) {
          throw (HandlerException) e;
        } else {
          throw new HandlerException(childExchange, e);
        }
      }
    }
  }

}
