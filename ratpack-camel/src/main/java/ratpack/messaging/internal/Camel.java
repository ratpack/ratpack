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

import com.google.common.collect.Maps;
import com.google.inject.Injector;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import ratpack.guice.Guice;
import ratpack.messaging.MessageContext;
import ratpack.messaging.MessageHandler;
import ratpack.messaging.MessageRequest;
import ratpack.messaging.MessageResponse;
import ratpack.registry.Registry;
import ratpack.util.internal.InternalRatpackError;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Camel {
  private final Registry registry;
  private final CamelContext context;

  @Inject
  public Camel(Injector injector, CamelContext context) {
    this.registry = Guice.justInTimeRegistry(injector);
    this.context = context;
  }

  public void init() {
    final Map<String, Set<MessageHandler>> map = Maps.newHashMap();

    for (CamelInitializer initializer : registry.getAll(CamelInitializer.class)) {
      initializer.init(context);
    }

    for (MessageHandler handler : registry.getAll(MessageHandler.class)) {
      if (!map.containsKey(handler.getUrl())) {
        map.put(handler.getUrl(), new CopyOnWriteArraySet<MessageHandler>());
      }
      map.get(handler.getUrl()).add(handler);
    }

    try {
      context.start();
      context.addRoutes(new RouteBuilder() {
        @Override
        public void configure() throws Exception {
          for (Map.Entry<String, Set<MessageHandler>> entry : map.entrySet()) {
            String url = entry.getKey();
            Set<MessageHandler> handlers = entry.getValue();
            from(url).process(new MessageHandlerAdapter(registry, handlers.toArray(new MessageHandler[handlers.size()])));
          }
        }
      });
    } catch (Exception e) {
      throw new InternalRatpackError("Failed to initialize Camel messaging infrastructure", e);
    }
  }

  static class MessageHandlerAdapter implements Processor {
    private final Registry registry;
    private final MessageHandler[] messageHandlers;

    MessageHandlerAdapter(Registry registry, MessageHandler[] messageHandlers) {
      this.messageHandlers = messageHandlers;
      this.registry = registry;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
      Map<String, String> headers = Maps.newHashMap();

      for (Map.Entry<String, Object> entry : exchange.getIn().getHeaders().entrySet()) {
        headers.put(entry.getKey(), entry.getValue().toString());
      }

      MessageRequest request = new DefaultMessageRequest(headers, exchange.getIn().getBody(String.class));
      MessageResponse response = new DefaultMessageResponse(exchange);
      MessageHandler handler = messageHandlers[0];
      MessageContext context = new DefaultMessageContext(registry, request, response, messageHandlers, 1, handler);

      handler.handle(context);
    }
  }

}
