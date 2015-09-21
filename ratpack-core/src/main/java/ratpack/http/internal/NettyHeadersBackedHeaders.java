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

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaders;
import ratpack.http.Headers;
import ratpack.util.MultiValueMap;
import ratpack.util.internal.ImmutableDelegatingMultiValueMap;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class NettyHeadersBackedHeaders implements Headers {

  protected final HttpHeaders headers;

  public NettyHeadersBackedHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public String get(CharSequence name) {
    return headers.getAsString(name);
  }

  @Override
  public String get(String name) {
    return headers.getAsString(name);
  }

  @Override
  public Date getDate(CharSequence name) {
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

  @Override
  public Date getDate(String name) {
    return getDate((CharSequence) name);
  }

  @Override
  public List<String> getAll(String name) {
    return headers.getAllAsString(name);
  }

  @Override
  public boolean contains(String name) {
    return headers.contains((CharSequence) name);
  }

  @Override
  public List<String> getAll(CharSequence name) {
    return headers.getAllAsString(name);
  }

  @Override
  public boolean contains(CharSequence name) {
    return headers.contains(name);
  }

  @SuppressWarnings("deprecation")
  @Override
  public Set<String> getNames() {
    return headers.names();
  }

  @Override
  public MultiValueMap<String, String> asMultiValueMap() {
    ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
    for (String s : getNames()) {
      builder.put(s, headers.getAllAsString(s));
    }
    return new ImmutableDelegatingMultiValueMap<>(builder.build());
  }

  @Override
  public HttpHeaders getNettyHeaders() {
    return headers;
  }
}
