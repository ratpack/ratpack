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

package org.ratpackframework.http.internal;

import io.netty.handler.codec.http.HttpHeaders;
import org.ratpackframework.http.Headers;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class NettyHeadersBackedHeaders implements Headers {

  protected final HttpHeaders headers;

  public NettyHeadersBackedHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  public String get(String name) {
    return headers.get(name);
  }

  public Date getDate(String name) {
    final String value = get(name);
    if (value == null) {
      return null;
    }

    try {
      return HttpHeaderDateFormat.get().parse(value);
    } catch (ParseException e) {
      return null;
    }
  }

  public List<String> getAll(String name) {
    return headers.getAll(name);
  }

  public boolean contains(String name) {
    return headers.contains(name);
  }

  public Set<String> getNames() {
    return headers.names();
  }

}
