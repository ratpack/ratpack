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
import ratpack.api.Nullable;
import ratpack.http.Headers;
import ratpack.util.MultiValueMap;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class DelegatingHeaders implements Headers {

  private final Headers headers;

  public DelegatingHeaders(Headers headers) {
    this.headers = headers;
  }

  @Nullable
  @Override
  public String get(CharSequence name) {
    return headers.get(name);
  }

  @Override
  @Nullable
  public String get(String name) {
    return headers.get(name);
  }

  @Override
  @Nullable
  public Date getDate(CharSequence name) {
    return headers.getDate(name);
  }

  @Override
  @Nullable
  public Date getDate(String name) {
    return headers.getDate(name);
  }

  @Override
  public List<String> getAll(CharSequence name) {
    return headers.getAll(name);
  }

  @Override
  public List<String> getAll(String name) {
    return headers.getAll(name);
  }

  @Override
  public boolean contains(CharSequence name) {
    return headers.contains(name);
  }

  @Override
  public boolean contains(String name) {
    return headers.contains(name);
  }

  @Override
  public Set<String> getNames() {
    return headers.getNames();
  }

  @Override
  public MultiValueMap<String, String> asMultiValueMap() {
    return headers.asMultiValueMap();
  }

  @Override
  public HttpHeaders getNettyHeaders() {
    return headers.getNettyHeaders();
  }
}
