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
import ratpack.handling.Redirector;


public class RedirectionHandler implements Handler {

  private final String location;
  private final int code;

  public RedirectionHandler(String location, int code) {
    this.location = location;
    this.code = code;
  }

  public void handle(Context context) {
    if (code < 300 || code >= 400) {
      context.error(new Exception("HTTP status code has to be >= 300 and <= 399"));
    } else {
      context.get(Redirector.class).redirect(context, location, code);
    }
  }
}
