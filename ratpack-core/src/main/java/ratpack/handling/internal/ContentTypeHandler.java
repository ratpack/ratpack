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

package ratpack.handling.internal;

import io.netty.handler.codec.http.HttpResponseStatus;
import ratpack.handling.Context;
import ratpack.handling.Handler;

public class ContentTypeHandler implements Handler {

  private final String[] contentTypes;

  public ContentTypeHandler(String... contentTypes) {
    this.contentTypes = contentTypes;
  }

  @Override
  public void handle(Context context) throws Exception {
    boolean accepted = false;
    String requestType = context.getRequest().getContentType().getType();
    if (requestType != null) {
      for (String contentType : contentTypes) {
        if (requestType.equals(contentType)) {
          accepted = true;
          break;
        }
      }
    }

    if (accepted) {
      context.next();
    } else {
      context.clientError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code());
    }
  }
}
