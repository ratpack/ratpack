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

import io.netty.handler.codec.http.HttpHeaders;
import ratpack.http.MutableHeaders;

import java.util.Date;

public class NettyHeadersBackedMutableHeaders extends NettyHeadersBackedHeaders implements MutableHeaders {

  public NettyHeadersBackedMutableHeaders(HttpHeaders headers) {
    super(headers);
  }

  public void add(String name, Object value) {
    headers.add(name, value);
  }

  public void set(String name, Object value) {
    headers.set(name, value);
  }

  @Override
  public void setDate(String name, Date value) {
    headers.set(name, HttpHeaderDateFormat.get().format(value));
  }

  public void set(String name, Iterable<?> values) {
    headers.set(name, values);
  }

  public void remove(String name) {
    headers.remove(name);
  }

  public void clear() {
    headers.clear();
  }

}
