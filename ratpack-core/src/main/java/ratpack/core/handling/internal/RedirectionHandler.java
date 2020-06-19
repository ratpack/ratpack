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

package ratpack.core.handling.internal;

import ratpack.core.handling.Context;
import ratpack.core.handling.Handler;

public class RedirectionHandler implements Handler {

  private final String location;
  private final int code;

  public RedirectionHandler(String location, int code) {
    this.location = location;
    if (code < 300 || code >= 400) {
      throw new IllegalArgumentException("redirect code must be 3xx, value is " + code);
    }
    this.code = code;
  }

  public void handle(Context context) {
    context.redirect(code, location);
  }
}
