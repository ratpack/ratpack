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

package org.ratpackframework.routing.internal;

import io.netty.channel.ChannelHandlerContext;
import org.ratpackframework.context.Context;
import org.ratpackframework.context.internal.ObjectHoldingHierarchicalContext;
import org.ratpackframework.error.ErrorHandlingContext;
import org.ratpackframework.file.FileSystemContext;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.path.PathContext;
import org.ratpackframework.util.CollectionUtils;

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
    next.handle(this);
  }

  public void next(Handler... handlers) {
    doNext(context, CollectionUtils.toList(handlers), next);
  }

  public void next(Iterable<Handler> handlers) {
    doNext(context, CollectionUtils.toList(handlers), next);
  }

  public void nextWithContext(Object object, Handler... handlers) {
    doNext(new ObjectHoldingHierarchicalContext(this.context, object), CollectionUtils.toList(handlers), next);
  }

  public void nextWithContext(Object object, Iterable<Handler> handlers) {
    doNext(new ObjectHoldingHierarchicalContext(this.context, object), CollectionUtils.toList(handlers), next);
  }

  public void nextWithContext(Context context, Handler... handlers) {
    doNext(context, CollectionUtils.toList(handlers), next);
  }

  public void nextWithContext(Context context, Iterable<Handler> handlers) {
    doNext(context, CollectionUtils.toList(handlers), next);
  }

  public Map<String, String> getPathTokens() {
    return get(PathContext.class).getTokens();
  }

  public Map<String, String> getAllPathTokens() {
    return get(PathContext.class).getAllTokens();
  }

  public File file(String... pathComponents) {
    return get(FileSystemContext.class).file(pathComponents);
  }

  public void error(Exception exception) {
    get(ErrorHandlingContext.class).error(this, exception);
  }

  protected void doNext(final Context context, final List<Handler> handlers, final Handler exhausted) {
    assert context != null;
    if (handlers.isEmpty()) {
      exhausted.handle(this);
    } else {
      Handler handler = handlers.remove(0);
      Handler nextHandler = new Handler() {
        public void handle(Exchange exchange) {
          ((DefaultExchange) exchange).doNext(context, handlers, exhausted);
        }
      };
      DefaultExchange childExchange = new DefaultExchange(request, response, channelHandlerContext, context, nextHandler);
      handler.handle(childExchange);
    }
  }

}
