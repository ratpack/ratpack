/*
 * Copyright 2014 the original author or authors.
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

import io.netty.handler.codec.http.*;

public class CustomHttpResponse extends DefaultHttpObject implements HttpResponse {

  private final HttpResponseStatus httpResponseStatus;
  private final HttpHeaders httpHeaders;

  public CustomHttpResponse(HttpResponseStatus httpResponseStatus, HttpHeaders httpHeaders) {
    this.httpResponseStatus = httpResponseStatus;
    this.httpHeaders = httpHeaders;
  }

  @Override
  public HttpResponseStatus status() {
    return httpResponseStatus;
  }

  @Override
  @Deprecated
  public HttpResponseStatus getStatus() {
    return status();
  }

  @Override
  @Deprecated
  public HttpVersion getProtocolVersion() {
    return protocolVersion();
  }

  @Override
  public HttpResponse setStatus(HttpResponseStatus status) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpResponse setProtocolVersion(HttpVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpVersion protocolVersion() {
    return HttpVersion.HTTP_1_1;
  }

  @Override
  public HttpHeaders headers() {
    return httpHeaders;
  }

}
