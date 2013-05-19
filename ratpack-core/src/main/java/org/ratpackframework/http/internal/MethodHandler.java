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

package org.ratpackframework.http.internal;

import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;

import java.util.Collection;
import java.util.List;

import static org.ratpackframework.util.internal.CollectionUtils.toUpperCase;

public class MethodHandler implements Handler {

  private final List<String> methods;
  private final Handler delegate;

  public MethodHandler(Collection<String> methods, Handler delegate) {
    this.methods = toUpperCase(methods);
    this.delegate = delegate;
  }

  public MethodHandler(List<String> methods, Handler delegate) {
    this.methods = toUpperCase(methods);
    this.delegate = delegate;
  }

  public void handle(Exchange exchange) {
    String methodName = exchange.getRequest().getMethod().getName();
    if (methods.contains(methodName)) {
      exchange.next(delegate);
    } else {
      exchange.clientError(405);
    }
  }
}
