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

import io.netty.handler.codec.http.HttpResponseStatus;
import ratpack.http.MutableStatus;

public class DefaultMutableStatus implements MutableStatus {

  private HttpResponseStatus responseStatus = HttpResponseStatus.OK;

  @Override
  public int getCode() {
    return responseStatus.code();
  }

  @Override
  public String getMessage() {
    return responseStatus.reasonPhrase();
  }

  public void set(int code) {
    responseStatus = HttpResponseStatus.valueOf(code);
  }

  public void set(int code, String message) {
    responseStatus = new HttpResponseStatus(code, message);
  }

  public HttpResponseStatus getResponseStatus() {
    return responseStatus;
  }

}
