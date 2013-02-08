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

package org.ratpackframework.groovy.routing.internal;

import groovy.lang.Closure;
import org.ratpackframework.Response;
import org.ratpackframework.Handler;

public class ClosureBackedResponseHandler implements Handler<Response> {

  private final Closure<?> closure;
  private int maximumNumberOfParameters;

  public ClosureBackedResponseHandler(Closure<?> closure) {
    this.closure = closure;
    this.maximumNumberOfParameters = closure.getMaximumNumberOfParameters();
  }

  @Override
  public void handle(Response response) {
    Closure<?> clone = (Closure<?>) closure.clone();
    clone.setDelegate(response);
    clone.setResolveStrategy(Closure.DELEGATE_FIRST);

    maximumNumberOfParameters = clone.getMaximumNumberOfParameters();
    switch (maximumNumberOfParameters) {
      case 0:
        clone.call();
        break;
      case 1:
        clone.call(response.getRequest());
        break;
      default:
        clone.call(response.getRequest(), response);
    }
  }
}
