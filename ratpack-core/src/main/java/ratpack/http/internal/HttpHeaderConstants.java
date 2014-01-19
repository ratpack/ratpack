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

import io.netty.handler.codec.http.HttpHeaders;

public abstract class HttpHeaderConstants {

  private HttpHeaderConstants() {
  }

  public static final CharSequence CONTENT_LENGTH = HttpHeaders.newEntity(HttpHeaders.Names.CONTENT_LENGTH);
  public static final CharSequence CONTENT_TYPE = HttpHeaders.newEntity(HttpHeaders.Names.CONTENT_TYPE);
  public static final CharSequence LAST_MODIFIED = HttpHeaders.newEntity(HttpHeaders.Names.LAST_MODIFIED);
  public static final CharSequence CONNECTION = HttpHeaders.newEntity(HttpHeaders.Names.CONNECTION);
  public static final CharSequence KEEP_ALIVE = HttpHeaders.newEntity(HttpHeaders.Values.KEEP_ALIVE);

}
