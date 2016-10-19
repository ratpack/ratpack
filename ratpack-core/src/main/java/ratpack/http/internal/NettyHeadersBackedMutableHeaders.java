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

import com.google.common.collect.Iterables;
import io.netty.handler.codec.http.HttpHeaders;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;

import java.time.Instant;
import java.util.Date;

public class NettyHeadersBackedMutableHeaders extends NettyHeadersBackedHeaders implements MutableHeaders {

  private static final com.google.common.base.Function<Object, Object> CONVERT_VALUE = v ->
    v instanceof Instant ? new Date(((Instant) v).toEpochMilli()) : v;

  public NettyHeadersBackedMutableHeaders(HttpHeaders headers) {
    super(headers);
  }

  @SuppressWarnings("deprecation")
  public MutableHeaders add(CharSequence name, Object value) {
    headers.add(name, CONVERT_VALUE.apply(value));
    return this;
  }

  @SuppressWarnings("deprecation")
  public MutableHeaders set(CharSequence name, Object value) {
    headers.set(name, CONVERT_VALUE.apply(value));
    return this;
  }

  @SuppressWarnings("deprecation")
  @Override
  public MutableHeaders setDate(CharSequence name, Date value) {
    headers.set(name, HttpHeaderDateFormat.get().format(value));
    return this;
  }

  @SuppressWarnings("deprecation")
  public MutableHeaders set(CharSequence name, Iterable<?> values) {
    headers.set(name, Iterables.transform(values, CONVERT_VALUE));
    return this;
  }

  @SuppressWarnings("deprecation")
  public MutableHeaders remove(CharSequence name) {
    headers.remove(name);
    return this;
  }

  public MutableHeaders clear() {
    headers.clear();
    return this;
  }

  @Override
  public MutableHeaders copy(Headers headers) {
    this.headers.add(headers.getNettyHeaders());

    return this;
  }

}
