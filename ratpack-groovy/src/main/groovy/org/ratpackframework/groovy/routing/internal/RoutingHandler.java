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

package org.ratpackframework.groovy.routing.internal;

import org.ratpackframework.Action;
import org.ratpackframework.groovy.routing.Routing;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

import java.util.LinkedList;
import java.util.List;

public class RoutingHandler implements Handler {

  private final Action<? super Routing> action;

  public RoutingHandler(Action<? super Routing> action) {
    this.action = action;
  }

  @Override
  public void handle(Exchange exchange) {
    List<Handler> handlers = new LinkedList<>();
    Routing routing = new DefaultRouting(exchange, handlers);
    action.execute(routing);
    exchange.next(handlers);
  }

}
