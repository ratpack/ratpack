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

package org.ratpackframework.guice.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

public class InjectingHandler implements Handler {

  private final Class<? extends Handler> handlerType;
  private final AbstractModule module;

  public InjectingHandler(final Class<? extends Handler> handlerType) {
    this.handlerType = handlerType;
    module = new AbstractModule() {
      @Override
      protected void configure() {
        bind(handlerType);
      }
    };
  }

  @Override
  public void handle(Exchange exchange) {
    // TODO what's the cost of creating an injector for every request
    //      if it's not negligible, we could support using an explicit injector instead of getting
    //      it from the request context.
    //
    //      Another option would be to cache the injector. If the injection context is the same, then just reuse.
    Injector injector = exchange.get(Injector.class);
    Injector childInjector = injector.createChildInjector(module);
    Handler instance = childInjector.getInstance(handlerType);
    instance.handle(exchange);
  }

}
