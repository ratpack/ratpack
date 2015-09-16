/*
 * Copyright 2015 the original author or authors.
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

package ratpack.handling.internal;

import ratpack.handling.RequestId;
import ratpack.http.Request;

public class HeaderBasedRequestIdGenerator implements RequestId.Generator {

  private final CharSequence headerName;
  private final RequestId.Generator fallback;

  public HeaderBasedRequestIdGenerator(CharSequence headerName, RequestId.Generator fallback) {
    this.headerName = headerName;
    this.fallback = fallback;
  }

  @Override
  public RequestId generate(Request request) {
    String value = request.getHeaders().get(headerName);
    if (value == null) {
      return fallback.generate(request);
    } else {
      return RequestId.of(value);
    }
  }
}
