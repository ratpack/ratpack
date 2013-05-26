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
import org.ratpackframework.context.Context;
import org.ratpackframework.error.ClientErrorHandler;
import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.ByMethodChain;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.path.PathBinding;

import java.io.File;
import java.util.List;
import java.util.Map;

public class DefaultExchange implements Exchange {

  private final Request request;
  private final Response response;

  private final ChannelHandlerContext channelHandlerContext;

  private final Handler next;
  private final Context context;

  public DefaultExchange(Request request, Response response, ChannelHandlerContext channelHandlerContext, Context context, Handler next) {
    this.request = request;
    this.response = response;
    this.channelHandlerContext = channelHandlerContext;
    this.context = context;
    this.next = next;
  }

  public Request getRequest() {
    return request;
  }

  public Response getResponse() {
    return response;
  }

  public Context getContext() {
    return context;
  }

  public <T> T get(Class<T> type) {
    return context.get(type);
  }

  public <T> T maybeGet(Class<T> type) {
    return context.maybeGet(type);
  }

  public void next() {
    try {
      next.handle(this);
    } catch (Exception e) {
      if (e instanceof HandlerException) {
        throw (HandlerException) e;
      } else {
        throw new HandlerException(this, e);
      }
    }
  }

  public void insert(List<Handler> handlers) {
    doNext(this, context, handlers, 0, next);
  }

  public void insert(Context context, List<Handler> handlers) {
    doNext(this, context, handlers, 0, next);
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
      error(e);
    }
  }

  public ByMethodChain getMethods() {
    return new DefaultByMethodChain(this);
  }

  protected void doNext(final Exchange parentExchange, final Context context, final List<Handler> handlers, final int index, final Handler exhausted) {
    assert context != null;
    if (index == handlers.size()) {
      exhausted.handle(parentExchange);
    } else {
      Handler handler = handlers.get(index);
      Handler nextHandler = new Handler() {
        public void handle(Exchange exchange) {
          ((DefaultExchange) exchange).doNext(parentExchange, context, handlers, index + 1, exhausted);
        }
      };
      DefaultExchange childExchange = new DefaultExchange(request, response, channelHandlerContext, context, nextHandler);
      handler.handle(childExchange);
    }
  }

}
