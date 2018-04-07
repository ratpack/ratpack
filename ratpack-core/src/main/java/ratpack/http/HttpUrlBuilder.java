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
 * Typically used to build URLs for use with the {@link ratpack.http.client.HttpClient}.
 * <pre class="java">{@code
 * import ratpack.http.HttpUrlBuilder;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   public static void main(String... args) {
 *     String url = HttpUrlBuilder.http()
 *       .host("foo.com")
 *       .path("a/b")
 *       .segment("c/%s", "d")
 *       .params("k1", "v1", "k2", "v2")
 *       .build()
 *       .toString();
 *
 *     assertEquals("http://foo.com/a/b/c%2Fd?k1=v1&k2=v2", url);
 *   }
 * }
 * }</pre>
 */
public interface HttpUrlBuilder {

  /**
   * Create a new builder, with the initial state of the given URI.
   * <p>
   * The URI must be of the {@code http} or {@code https} protocol.
   * If it is of any other, an {@link IllegalArgumentException} will be thrown.
   *
   * @param uri the uri to base the builder's state from
   * @return a new url builder
   */
  static HttpUrlBuilder base(URI uri) {
    return new DefaultHttpUrlBuilder(uri);
  }

  /**
   * Create a new HTTP URL builder.
   * <p>
   * The protocol is set to HTTP, host to {@code localhost} and port to the default for the protocol.
   *
   * @return a new url builder
   */
  static HttpUrlBuilder http() {
    return new DefaultHttpUrlBuilder();
  }

  /**
   * Create a new HTTPS URL builder.
   * <p>
   * The protocol is set to HTTPS, host to {@code localhost} and port to the default for the protocol.
   *
   * @return a new url builder
   */
  static HttpUrlBuilder https() {
    return http().secure();
  }

  /**
   * Sets the protocol to be HTTPS.
   * <p>
   * If the port has not been explicitly set, it will be changed to match the default for HTTPS.
   *
   * @return {@code this}
   */
  HttpUrlBuilder secure();

  /**
   * Sets the host to the given value.
   *
   * @param host The host.
   * @return {@code this}
   */
  HttpUrlBuilder host(String host);

  /**
   * Sets the port to the given value.
   * <p>
   * Any value less than 1 will throw an {@link IllegalAccessException}.
   *
   * @param port The port number.
   * @return {@code this}
   */
  HttpUrlBuilder port(int port);

  /**
   * Appends the path to the URL.
   * <p>
   * The given value will may be a string such as {@code "foo"} or {@code "foo/bar"}.
   * In the case of the latter, the {@code "/"} character will not be URL escaped.
   * All other meta characters that apply to the path component of a HTTP URL will be percent encoded.
   * <p>
   * If the value to append should be exactly one path segment (i.e. {@code "/"} should be encoded), use {@link #segment(String, Object...)}.
   *
   * @param path the path to append
   * @return {@code this}
   */
  HttpUrlBuilder path(String path);

  /**
   * Appends the path to the URL, unless it is empty or {@code null}.
   * <p>
   * Has the same result as {@link #path(String)}, except that empty or {@code null} values are ignored.
   *
   * @param path the path to append
   * @return {@code this}
   * @since 1.2
   */
  default HttpUrlBuilder maybePath(String path) {
    if (path != null && !path.isEmpty()) {
      path(path);
    }
    return this;
  }

  /**
   * Appends the path to the URL, without escaping any meta characters.
   * <p>
   * This can be used when it is guaranteed that the value has already been suitably encoded.
   * <p>
   * If the value has not already been encoded, use {@link #path(String)}
   *
   * @param path the path to append
   * @return {@code this}
   * @since 1.2
   */
  HttpUrlBuilder encodedPath(String path);

  /**
   * Appends the path to the URL, without escaping any meta characters, unless it is empty or {@code null}.
   * <p>
   * Has the same result as {@link #encodedPath(String)}, except that empty or {@code null} values are ignored.
   *
   * @param path the path to append
   * @return {@code this}
   * @since 1.2
   */
  default HttpUrlBuilder maybeEncodedPath(String path) {
    if (path != null && !path.isEmpty()) {
      encodedPath(path);
    }
    return this;
  }
  /**
   * Appends one path segment to the URL.
   * <p>
   * This method first builds a string using {@link String#format(String, Object...)}.
   * The resultant string is percent encoded as is applicable for the path component of a HTTP URL.
   * <p>
   * This method should generally be preferred over {@link #path(String)} when incrementally building dynamic URLs, where values may container the HTTP URL path delimiter (i.e. {@code "/"}).
   *
   * @param pathSegment the path segment format string
   * @param args token arguments
   * @return {@code this}
   */
  HttpUrlBuilder segment(String pathSegment, Object... args);

  /**
   * Add some query params to the URL.
   * <p>
   * Counting from zero, even numbered items are considered keys and odd numbered items are considered values.
   * If param list ends in a key, a subsequent value of {@code ""} will be implied.
   *
   * @param params the param list
   * @return {@code this}
   */
  HttpUrlBuilder params(String... params);

  /**
   * Add some query params to the URL.
   * <p>
   * The given action will be supplied with a multi map builder, to which it can contribute query params.
   * <p>
   * This method is additive with regard to the query params of this builder.
   *
   * @param params an action that contributes query params
   * @return {@code this}
   * @throws Exception any thrown by {@code params}
   */
  default HttpUrlBuilder params(Action<? super ImmutableMultimap.Builder<String, Object>> params) throws Exception {
    return params(Action.with(ImmutableMultimap.builder(), params).build());
  }

  /**
   * Add some query params to the URL.
   * <p>
   * The entries of the given map are added as query params to the URL being built.
   * <p>
   * This method is additive with regard to the query params of this builder.
   *
   * @param params a map of query params to add to the URL being built
   * @return {@code this}
   */
  HttpUrlBuilder params(Map<String, ?> params);

  /**
   * Add some query params to the URL.
   * <p>
   * The entries of the given multi map are added as query params to the URL being built.
   * <p>
   * This method is additive with regard to the query params of this builder.
   *
   * @param params a multi map of query params to add to the URL being built
   * @return {@code this}
   */
  HttpUrlBuilder params(Multimap<String, ?> params);

  /**
   * Add some query params to the URL.
   * <p>
   * The entries of the given multi value map are added as query params to the URL being built.
   * <p>
   * This method is additive with regard to the query params of this builder.
   *
   * @param params a multi value map of query params to add to the URL being built
   * @return {@code this}
   * @since 1.2
   */
  HttpUrlBuilder params(MultiValueMap<String, ?> params);

  /**
   * Add a fragment to the URL
   * @param fragment string of the fragment
   * @return {@code this}
   * @since 1.6
   */
  HttpUrlBuilder fragment(String fragment);

  /**
   * Builds the URI based on this builder's current state.
   *
   * @return a new HTTP/HTTPS URI
   */
  URI build();

}
