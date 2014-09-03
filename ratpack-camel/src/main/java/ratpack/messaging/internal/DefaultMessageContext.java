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

package ratpack.messaging.internal;

import ratpack.messaging.MessageContext;
import ratpack.messaging.MessageHandler;
import ratpack.messaging.MessageRequest;
import ratpack.messaging.MessageResponse;
import ratpack.registry.Registries;
import ratpack.registry.Registry;

import java.util.Arrays;

public class DefaultMessageContext implements MessageContext {
  private final Registry registry;
  private final MessageRequest request;
  private final MessageResponse response;
  private final MessageHandler[] messageHandlers;
  private final MessageHandler exhausted;
  private int nextIndex;

  DefaultMessageContext(Registry registry, MessageRequest request, MessageResponse response,
                        MessageHandler[] messageHandlers, int nextIndex, MessageHandler exhausted) {
    this.registry = registry;
    this.request = request;
    this.response = response;
    this.messageHandlers = messageHandlers;
    this.nextIndex = nextIndex;
    this.exhausted = exhausted;
  }

  @Override
  public MessageContext getContext() {
    return this;
  }

  @Override
  public <O> O get(Class<O> type) {
    return registry.get(type);
  }

  @Override
  public MessageRequest getRequest() {
    return request;
  }

  @Override
  public MessageResponse getResponse() {
    return response;
  }

  @Override
  public void next() {
    if (nextIndex < Arrays.asList(messageHandlers).size()) {
      doNext(this, registry, messageHandlers, nextIndex, exhausted);
    }
  }

  @Override
  public void insert(MessageHandler... handlers) {
    doNext(this, registry, messageHandlers, 0, exhausted);
  }

  @Override
  public void insert(Registry registry, MessageHandler... handlers) {
    Registry joinedRegistry = Registries.join(this.registry, registry);
    doNext(this, joinedRegistry, messageHandlers, 0, new RejoinMessageHandler());
  }

  private class RejoinMessageHandler implements MessageHandler {
    public String getUrl() {
      return exhausted.getUrl();
    }

    @Override
    public void handle(MessageContext context) {
      doNext(DefaultMessageContext.this, registry, messageHandlers, nextIndex, exhausted);
    }
  }

  protected void doNext(MessageContext parentContext, Registry registry,
                        final MessageHandler[] nextHandlers, int nextIndex, MessageHandler exhausted) {
    MessageContext context;
    MessageHandler handler;

    if (nextIndex >= nextHandlers.length) {
      context = parentContext;
      handler = exhausted;
    } else {
      handler = nextHandlers[nextIndex];
      context = createContext(registry, nextHandlers, nextIndex, exhausted);
    }

    try {
      handler.handle(context);
    } catch (Throwable e) {
      if (e instanceof HandlerException) {
        throw (HandlerException) e;
      } else {
        throw new HandlerException(e);
      }

    }
  }

  private MessageContext createContext(Registry registry, MessageHandler[] nextHandlers, int nextIndex, MessageHandler exhausted) {
    return new DefaultMessageContext(registry, request, response, nextHandlers, nextIndex+1, exhausted);
  }

  private static class HandlerException extends Error {
    private static final long serialVersionUID = 0;

    private HandlerException(Throwable cause) {
      super(cause);
    }
  }
}
