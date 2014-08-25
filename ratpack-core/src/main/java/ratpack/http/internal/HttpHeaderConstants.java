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
  public static final CharSequence CONTENT_ENCODING = HttpHeaders.newEntity(HttpHeaders.Names.CONTENT_ENCODING);
  public static final CharSequence TRANSFER_ENCODING = HttpHeaders.newEntity(HttpHeaders.Names.TRANSFER_ENCODING);
  public static final CharSequence CHUNKED = HttpHeaders.newEntity(HttpHeaders.Values.CHUNKED);
  public static final CharSequence CACHE_CONTROL = HttpHeaders.newEntity(HttpHeaders.Names.CACHE_CONTROL);
  public static final CharSequence PRAGMA = HttpHeaders.newEntity(HttpHeaders.Names.PRAGMA);
  public static final CharSequence NO_CACHE = HttpHeaders.newEntity(HttpHeaders.Values.NO_CACHE);
  public static final CharSequence NO_STORE = HttpHeaders.newEntity(HttpHeaders.Values.NO_STORE);
  public static final CharSequence MUST_REVALIDATE = HttpHeaders.newEntity(HttpHeaders.Values.MUST_REVALIDATE);
  public static final CharSequence MAX_AGE = HttpHeaders.newEntity(HttpHeaders.Values.MAX_AGE);
  public static final CharSequence HOST = HttpHeaders.newEntity(HttpHeaders.Names.HOST);
  public static final CharSequence COOKIE = HttpHeaders.newEntity(HttpHeaders.Names.COOKIE);
  public static final CharSequence X_FORWARDED_HOST = HttpHeaders.newEntity("X-Forwarded-Host");
  public static final CharSequence X_FORWARDED_PROTO = HttpHeaders.newEntity("X-Forwarded-Proto");
  public static final CharSequence X_FORWARDED_SSL = HttpHeaders.newEntity("X-Forwarded-Ssl");

  public static final CharSequence UTF_8_TEXT = HttpHeaders.newEntity("text/plain;charset=UTF-8");
  public static final CharSequence ON = HttpHeaders.newEntity("on");

}
