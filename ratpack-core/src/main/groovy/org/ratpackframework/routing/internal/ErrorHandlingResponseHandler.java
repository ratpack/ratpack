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

package org.ratpackframework.routing.internal;

import org.ratpackframework.Response;
import org.ratpackframework.Handler;

public class ErrorHandlingResponseHandler implements Handler<Response> {

  private final Handler<Response> delegate;

  public ErrorHandlingResponseHandler(Handler<Response> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(Response response) {
    try {
      delegate.handle(response);
    } catch (Exception e) {
      response.error(e);
    }
  }

}
