/*
 * Copyright 2016 the original author or authors.
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
package ratpack.http.client.internal;

import ratpack.http.client.HttpClientRequestInterceptor;
import ratpack.http.client.SentRequest;

class HttpClientRequestInterceptorChain {
  private Iterable<? extends HttpClientRequestInterceptor> interceptors;

  /**
   * Constructor.
   *
   * @param interceptors request interceptors
   */
  HttpClientRequestInterceptorChain(final Iterable<? extends HttpClientRequestInterceptor>
                                             interceptors) {
    this.interceptors = interceptors;
  }

  void intercept(final SentRequest request) {
    interceptors.forEach(c -> c.intercept(new ImmutableSentRequest(request.method(),
      request.uri(),
      request.requestHeaders())));
  }
}
