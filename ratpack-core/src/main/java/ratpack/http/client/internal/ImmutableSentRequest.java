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


import io.netty.handler.codec.http.HttpHeaders;
import ratpack.http.client.SentRequest;

class ImmutableSentRequest implements SentRequest {
  private final String method;
  private final String uri;
  private final HttpHeaders headers;

  /**
   * Constructor.
   *
   * @param method the method
   * @param uri the uri
   * @param headers the headers
   */
  ImmutableSentRequest(final String method, final String uri, final HttpHeaders headers) {
    this.method = method;
    this.uri = uri;
    this.headers = headers;
  }

  @Override
  public String method() {
    return null;
  }

  @Override
  public String uri() {
    return null;
  }

  @Override
  public HttpHeaders requestHeaders() {
    return null;
  }
}
