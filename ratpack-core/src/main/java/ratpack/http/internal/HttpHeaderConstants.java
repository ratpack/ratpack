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

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.AsciiString;

public abstract class HttpHeaderConstants {

  private HttpHeaderConstants() {
  }

  public static final CharSequence CONTENT_LENGTH = HttpHeaderNames.CONTENT_LENGTH;
  public static final CharSequence CONTENT_TYPE = HttpHeaderNames.CONTENT_TYPE;
  public static final CharSequence ACCEPT = HttpHeaderNames.ACCEPT;
  public static final CharSequence LAST_MODIFIED = HttpHeaderNames.LAST_MODIFIED;
  public static final CharSequence CONNECTION = HttpHeaderNames.CONNECTION;
  public static final CharSequence CLOSE = HttpHeaderValues.CLOSE;
  public static final CharSequence KEEP_ALIVE = HttpHeaderValues.KEEP_ALIVE;
  public static final CharSequence CONTENT_ENCODING = HttpHeaderNames.CONTENT_ENCODING;
  public static final CharSequence IDENTITY = HttpHeaderValues.IDENTITY;
  public static final CharSequence TRANSFER_ENCODING = HttpHeaderNames.TRANSFER_ENCODING;
  public static final CharSequence CHUNKED = HttpHeaderValues.CHUNKED;
  public static final CharSequence CACHE_CONTROL = HttpHeaderNames.CACHE_CONTROL;
  public static final CharSequence PRAGMA = HttpHeaderNames.PRAGMA;
  public static final CharSequence NO_CACHE = HttpHeaderValues.NO_CACHE;
  public static final CharSequence NO_STORE = HttpHeaderValues.NO_STORE;
  public static final CharSequence MAX_AGE = HttpHeaderValues.MAX_AGE;
  public static final CharSequence MUST_REVALIDATE = HttpHeaderValues.MUST_REVALIDATE;
  public static final CharSequence NO_CACHE_FULL = new AsciiString(NO_CACHE + ", " + NO_STORE + ", " + MAX_AGE + "=0, " + MUST_REVALIDATE);
  public static final CharSequence HOST = HttpHeaderNames.HOST;
  public static final CharSequence COOKIE = HttpHeaderNames.COOKIE;
  public static final CharSequence SET_COOKIE = HttpHeaderNames.SET_COOKIE;
  public static final CharSequence ALLOW = HttpHeaderNames.ALLOW;
  public static final CharSequence LOCATION = HttpHeaderNames.LOCATION;

  public static final CharSequence X_FORWARDED_HOST = new AsciiString("X-Forwarded-Host");
  public static final CharSequence X_FORWARDED_PROTO = new AsciiString("X-Forwarded-Proto");
  public static final CharSequence X_FORWARDED_SSL = new AsciiString("X-Forwarded-Ssl");
  public static final CharSequence X_REQUESTED_WITH = new AsciiString("X-Requested-With");

  public static final CharSequence PLAIN_TEXT_UTF8 = new AsciiString("text/plain;charset=UTF-8");
  public static final CharSequence OCTET_STREAM = new AsciiString("application/octet-stream");
  public static final CharSequence JSON = new AsciiString("application/json");
  public static final CharSequence HTML_UTF_8 = new AsciiString("text/html;charset=UTF-8");
  public static final CharSequence ON = new AsciiString("on");

  public static final CharSequence TEXT_EVENT_STREAM_CHARSET_UTF_8 = new AsciiString("text/event-stream;charset=UTF-8");

  public static final String XML_HTTP_REQUEST = "XMLHttpRequest";
}
