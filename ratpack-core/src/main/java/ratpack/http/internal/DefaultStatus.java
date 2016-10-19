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

import io.netty.handler.codec.http.HttpResponseStatus;
import ratpack.http.Status;

public class DefaultStatus implements Status {

  private final HttpResponseStatus status;

  public DefaultStatus(HttpResponseStatus status) {
    this.status = status;
  }

  @Override
  public int getCode() {
    return status.code();
  }

  @Override
  public String getMessage() {
    return status.reasonPhrase();
  }

  @Override
  public HttpResponseStatus getNettyStatus() {
    return status;
  }

  @Override
  public String toString() {
    return status.code() + ":" + status.reasonPhrase();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultStatus that = (DefaultStatus) o;
    return status.equals(that.status);
  }

  @Override
  public int hashCode() {
    return status.hashCode();
  }
}
