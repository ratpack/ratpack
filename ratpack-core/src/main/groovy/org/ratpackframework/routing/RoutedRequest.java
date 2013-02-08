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

package org.ratpackframework.routing;

import org.ratpackframework.handler.Handler;
import org.ratpackframework.handler.HttpExchange;

public class RoutedRequest {

  private final HttpExchange exchange;
  private final Handler<? super RoutedRequest> next;

  public RoutedRequest(HttpExchange exchange, Handler<? super RoutedRequest> next) {
    this.exchange = exchange;
    this.next = next;
  }

  public void next() {
    next.handle(this);
  }

  public <T extends RoutedRequest> void next(T nextReplacement) {
    next.handle(nextReplacement);
  }

  public HttpExchange getExchange() {
    return exchange;
  }

}
