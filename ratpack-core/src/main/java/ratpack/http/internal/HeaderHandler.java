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

package ratpack.http.internal;

import ratpack.handling.Context;
import ratpack.handling.Handler;

public class HeaderHandler implements Handler {

  private final String headerName;
  private final String headerValue;
  private final Handler handler;

  public HeaderHandler(String headerName, String headerValue, Handler handler) {
    this.headerName = headerName;
    this.headerValue = headerValue;
    this.handler = handler;
  }

  public void handle(Context context) throws Exception {
    String value = context.getRequest().getHeaders().get(headerName);
    if (value != null && value.equals(headerValue)) {
      handler.handle(context);
    } else {
      context.next();
    }
  }
}
