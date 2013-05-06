/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.http.internal;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.ratpackframework.context.Context;
import org.ratpackframework.http.Exchange;
import org.ratpackframework.http.Handler;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.path.PathContext;
import org.ratpackframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

public class DefaultHttpExchange implements Exchange {

  private final Request request;
  private final Response response;

  private final ChannelHandlerContext channelHandlerContext;

  private final Handler next;
  private final Context context;

  public DefaultHttpExchange(Request request, Response response, ChannelHandlerContext channelHandlerContext, Context context, Handler next) {
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

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public void next() {
    next.handle(this);
  }

  public void next(Handler... handlers) {
    doNext(context, CollectionUtils.toList(handlers), next);
  }

  public void next(Iterable<Handler> handlers) {
    doNext(context, CollectionUtils.toList(handlers), next);
  }

  @Override
  public void nextWithContext(Object context, Handler... handlers) {
    doNext(this.context.push(context), CollectionUtils.toList(handlers), next);
  }

  @Override
  public void nextWithContext(Object context, Iterable<Handler> handlers) {
    doNext(this.context.push(context), CollectionUtils.toList(handlers), next);
  }

  @Override
  public Map<String, String> getPathTokens() {
    return getContext().require(PathContext.class).getTokens();
  }

  @Override
  public Map<String, String> getAllPathTokens() {
    return getContext().require(PathContext.class).getAllTokens();
  }

  protected void doNext(final Context context, final List<Handler> handlers, final Handler exhausted) {
    assert context != null;
    if (handlers.isEmpty()) {
      exhausted.handle(this);
    } else {
      Handler handler = handlers.remove(0);
      Handler nextHandler = new Handler() {
        @Override
        public void handle(Exchange exchange) {
          ((DefaultHttpExchange) exchange).doNext(context, handlers, exhausted);
        }
      };
      DefaultHttpExchange childExchange = new DefaultHttpExchange(request, response, channelHandlerContext, context, nextHandler);
      handler.handle(childExchange);
    }
  }

}
