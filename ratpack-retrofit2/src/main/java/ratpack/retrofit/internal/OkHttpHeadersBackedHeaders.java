/*
 * Copyright 2018 the original author or authors.
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

package ratpack.retrofit.internal;

import com.google.common.collect.ListMultimap;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;
import ratpack.api.Nullable;
import ratpack.http.Headers;
import ratpack.util.MultiValueMap;
import ratpack.util.internal.ImmutableDelegatingMultiValueMap;

import java.util.*;

public class OkHttpHeadersBackedHeaders implements Headers {

  private final okhttp3.Headers headers;

  public OkHttpHeadersBackedHeaders(okhttp3.Headers headers) {
    this.headers = headers;
  }

  @Nullable
  @Override
  public String get(CharSequence name) {
    return headers.get(name.toString());
  }

  @Nullable
  @Override
  public String get(String name) {
    return headers.get(name);
  }

  @Nullable
  @Override
  public Date getDate(CharSequence name) {
    return headers.getDate(name.toString());
  }

  @Nullable
  @Override
  public Date getDate(String name) {
    return headers.getDate(name);
  }

  @Override
  public List<String> getAll(CharSequence name) {
    return headers.values(name.toString());
  }

  @Override
  public List<String> getAll(String name) {
    return headers.values(name);
  }

  @Override
  public boolean contains(CharSequence name) {
    return headers.toMultimap().containsKey(name.toString());
  }

  @Override
  public boolean contains(String name) {
    return headers.toMultimap().containsKey(name);
  }

  @Override
  public Set<String> getNames() {
    return headers.toMultimap().keySet();
  }

  @Override
  public HttpHeaders getNettyHeaders() {
    ListMultimap<String, String> map = asMultiValueMap().asMultimap();
    CharSequence[] values = new CharSequence[map.size() * 2];
    Collection<Map.Entry<String, String>> entries = map.entries();
    int i = 0;
    Iterator<Map.Entry<String, String>> iter = entries.iterator();
    while(iter.hasNext()) {
      Map.Entry<String, String> entry = iter.next();
      values[i] = entry.getKey();
      values[i+1] = entry.getValue();
      i+=2;
    }
    return new ReadOnlyHttpHeaders(false, values);
  }

  @Override
  public MultiValueMap<String, String> asMultiValueMap() {
    return new ImmutableDelegatingMultiValueMap<>(headers.toMultimap());
  }
}
