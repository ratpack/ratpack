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

package ratpack.http;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import ratpack.func.Action;
import ratpack.http.internal.DefaultHttpUrlBuilder;
import ratpack.util.MultiValueMap;

import java.net.URI;
import java.util.Map;

/**
 * Builds a HTTP URL, safely.
 * <p>
 * This builder applies appropriate escaping of values to produce valid HTTP URLs.
 * <p>
 * Can be used to build URLs for use with the {@link ratpack.http.client.HttpClient}.
 */
public interface HttpUrlBuilder {

  static HttpUrlBuilder base(URI uri) {
    return new DefaultHttpUrlBuilder(uri);
  }

  static HttpUrlBuilder http() {
    return new DefaultHttpUrlBuilder();
  }

  static HttpUrlBuilder https() {
    return http().secure();
  }

  HttpUrlBuilder secure();

  HttpUrlBuilder host(String host);

  HttpUrlBuilder port(int port);

  HttpUrlBuilder path(String path);

  HttpUrlBuilder segment(String pathSegment, Object... args);

  HttpUrlBuilder params(String... params);

  default HttpUrlBuilder params(Action<? super ImmutableMultimap.Builder<String, Object>> params) throws Exception {
    return params(Action.with(ImmutableMultimap.builder(), params).build());
  }

  HttpUrlBuilder params(Map<String, ?> params);

  HttpUrlBuilder params(Multimap<String, ?> params);

  HttpUrlBuilder params(MultiValueMap<String, ?> params);

  URI build();

}
