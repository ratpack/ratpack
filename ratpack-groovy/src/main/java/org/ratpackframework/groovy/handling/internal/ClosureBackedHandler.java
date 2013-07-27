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

import groovy.lang.Closure;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.ServiceExtractor;

import java.util.List;

public class ClosureBackedHandler implements Handler {

  private final Closure<?> closure;
  private final List<Class<?>> parameterTypes;
  private final boolean supportsExhangeParam;

  public ClosureBackedHandler(Closure<?> closure) {
    this.closure = (Closure<?>) closure.clone();
    this.supportsExhangeParam = closure.getMaximumNumberOfParameters() > 0;
    closure.setDelegate(null);

    this.parameterTypes = ClosureHandlerParameterListInspector.retrieveParameterTypes(this.closure);
  }

  public void handle(Exchange exchange) {
    Closure<?> exchangeInstance = (Closure<?>) closure.clone();
    exchangeInstance.setDelegate(exchange);
    exchangeInstance.setResolveStrategy(Closure.DELEGATE_ONLY);

    if (parameterTypes.isEmpty()) {
      if (supportsExhangeParam) {
        exchangeInstance.call(exchange);
      } else {
        exchangeInstance.call();
      }
    } else {
      Object[] services = ServiceExtractor.extract(parameterTypes, exchange);
      exchangeInstance.call(services);
    }
  }
}
