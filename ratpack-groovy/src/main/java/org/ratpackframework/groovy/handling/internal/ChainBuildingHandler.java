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

package org.ratpackframework.groovy.handling.internal;

import org.ratpackframework.groovy.handling.Chain;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.util.Action;

import java.util.LinkedList;
import java.util.List;

public class ChainBuildingHandler implements Handler {

  private final List<Handler> handlers;

  public ChainBuildingHandler(Action<? super Chain> action) {
    handlers = new LinkedList<Handler>();
    Chain chainBuilder = new DefaultChain(handlers);
    action.execute(chainBuilder);
  }

  public void handle(Exchange exchange) {
    exchange.next(handlers);
  }

}
