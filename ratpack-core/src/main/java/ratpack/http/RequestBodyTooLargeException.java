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

package ratpack.http;

import io.netty.handler.codec.http.HttpResponseStatus;

public class RequestBodyTooLargeException extends ClientErrorException {

  private final long maxContentLength;
  private final long receivedContentLength;

  public RequestBodyTooLargeException(long maxContentLength, long receivedContentLength) {
    super("the request content length of " + receivedContentLength + " exceeded the allowed maximum of " + maxContentLength);
    this.maxContentLength = maxContentLength;
    this.receivedContentLength = receivedContentLength;
  }

  public long getMaxContentLength() {
    return maxContentLength;
  }

  public long getReceivedContentLength() {
    return receivedContentLength;
  }

  @Override
  public int getClientErrorCode() {
    return HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.code();
  }

}
