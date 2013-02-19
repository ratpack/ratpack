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
import org.ratpackframework.http.HttpExchange;

public class RoutedHttpExchange implements Routed<HttpExchange> {

  private final HttpExchange exchange;
  private final Handler<? super RoutedHttpExchange> next;

  public RoutedHttpExchange(HttpExchange exchange, Handler<? super RoutedHttpExchange> next) {
    this.exchange = exchange;
    this.next = next;
  }

  @Override
  public HttpExchange get() {
    return exchange;
  }

  @Override
  public void next() {
    next.handle(this);
  }

  @Override
  public Routed<HttpExchange> withNext(Handler<Routed<HttpExchange>> next) {
    return new RoutedHttpExchange(exchange, next);
  }

  @Override
  public void error(Exception e) {
    exchange.error(e);
  }

}
