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

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import io.netty.handler.codec.http.QueryStringDecoder;
import ratpack.http.HttpUrlBuilder;
import ratpack.util.MultiValueMap;
import ratpack.util.internal.InternalRatpackError;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;

public class DefaultHttpUrlBuilder implements HttpUrlBuilder {

  private static final CharMatcher HOST_NAME_ILLEGAL_CHARS = CharMatcher.inRange('a', 'z')
    .or(CharMatcher.inRange('A', 'Z'))
    .or(CharMatcher.inRange('0', '9'))
    .or(CharMatcher.anyOf(".-"))
    .negate()
    .precomputed();

  private static final Joiner PATH_JOINER = Joiner.on("/");
  public static final Escaper PATH_SEGMENT_ESCAPER = UrlEscapers.urlPathSegmentEscaper();

  private String protocol = "http";
  private String host = "localhost";
  private int port = -1;
  private final List<String> pathSegments = new ArrayList<>();
  private final Multimap<String, Object> params = MultimapBuilder.linkedHashKeys().linkedListValues().build();
  private boolean hasTrailingSlash;
  private String fragment;

  public DefaultHttpUrlBuilder() {
  }

  public DefaultHttpUrlBuilder(URI uri) {
    this.protocol = uri.getScheme();
    if (protocol == null) {
      protocol = "http";
    }

    protocol = protocol.toLowerCase();
    if (!protocol.equals("http") && !protocol.equals("https")) {
      throw new IllegalArgumentException("uri " + uri + " must be a http or https uri");
    }

    host(uri.getHost());
    port(uri.getPort());
    String rawPath = uri.getRawPath();
    if (rawPath != null && !rawPath.isEmpty() && !rawPath.equals("/")) {
      String[] parts = rawPath.substring(1).split("/");
      for (String part : parts) {
        try {
          // have to encode + to stop URLDecoder from treating it as a space (it's only synonymous with %20 in query strings)
          String s = part.replaceAll("\\+", "%2B");
          segment(URLDecoder.decode(s, "UTF8"));
        } catch (UnsupportedEncodingException e) {
          throw new InternalRatpackError("UTF8 is not available", e);
        }
      }
      hasTrailingSlash = rawPath.substring(rawPath.length() - 1, rawPath.length()).equals("/");
    }

    if (uri.getRawQuery() != null) {
      new QueryStringDecoder(uri).parameters().forEach(params::putAll);
    }

    fragment = uri.getFragment();

  }

  @Override
  public HttpUrlBuilder secure() {
    this.protocol = "https";
    return this;
  }

  @Override
  public HttpUrlBuilder host(String host) {
    // http://en.wikipedia.org/wiki/Hostname#Restrictions%5Fon%5Fvalid%5Fhost%5Fnames
    int indexIn = HOST_NAME_ILLEGAL_CHARS.indexIn(host);
    if (indexIn >= 0) {
      throw new IllegalArgumentException("character '" + host.charAt(indexIn) + "' of host name '" + host + "' is invalid (only [a-zA-Z0-9.-] are allowed in host names)");
    }
    this.host = host;
    return this;
  }

  @Override
  public HttpUrlBuilder port(int port) {
    if (port == 0 || port < -1) {
      throw new IllegalArgumentException("port must be greater than 0 or exactly -1, is " + port);
    }
    this.port = port;
    return this;
  }

  @Override
  public HttpUrlBuilder encodedPath(String path) {
    Objects.requireNonNull(path, "path must not be null");
    Arrays.asList(path.split("/")).forEach(pathSegments::add);
    return this;
  }

  @Override
  public HttpUrlBuilder path(String path) {
    Objects.requireNonNull(path, "path must not be null");
    Arrays.asList(path.split("/")).forEach(this::segment);
    return this;
  }

  @Override
  public HttpUrlBuilder segment(String pathSegment, Object... args) {
    Objects.requireNonNull(pathSegment, "pathSegment must not be null");
    if (args.length == 0) {
      pathSegments.add(PATH_SEGMENT_ESCAPER.escape(pathSegment));
    } else {
      pathSegments.add(PATH_SEGMENT_ESCAPER.escape(String.format(pathSegment, args)));
    }
    return this;
  }

  @Override
  public HttpUrlBuilder params(String... params) {
    int i = 0;
    while (i < params.length) {
      String key = params[i];
      String value = "";
      if (++i < params.length) {
        value = params[i++];
      }

      this.params.put(key, value);
    }

    return this;
  }

  @Override
  public HttpUrlBuilder params(Map<String, ?> params) {
    for (Map.Entry<String, ?> entry : params.entrySet()) {
      this.params.put(entry.getKey(), entry.getValue());
    }

    return this;
  }

  @Override
  public HttpUrlBuilder params(Multimap<String, ?> params) {
    this.params.putAll(params);
    return this;
  }

  @Override
  public HttpUrlBuilder params(MultiValueMap<String, ?> params) {
    this.params.putAll(params.asMultimap());
    return this;
  }

  @Override
  public HttpUrlBuilder fragment(String fragment) {
    this.fragment = fragment;
    return this;
  }

  @Override
  public URI build() {
    String string = toString();

    try {
      return new URI(string);
    } catch (URISyntaxException e) {
      throw new InternalRatpackError("HttpUriBuilder produced invalid URI: " + toString(), e);
    }
  }

  private void appendPathString(StringBuilder stringBuilder) {
    if (!pathSegments.isEmpty()) {
      stringBuilder.append("/");
      PATH_JOINER.appendTo(stringBuilder, pathSegments);
    }
  }

  private void appendQueryString(StringBuilder stringBuilder) {
    if (!params.isEmpty()) {
      stringBuilder.append("?");
      Iterator<Map.Entry<String, Object>> parts = params.entries().iterator();
      if (parts.hasNext()) {
        Map.Entry<String, Object> entry = parts.next();
        stringBuilder.append(UrlEscapers.urlFormParameterEscaper().escape(entry.getKey()));
        String value = entry.getValue().toString();
        if (value != null && value.length() > 0) {
          stringBuilder.append("=");
          stringBuilder.append(UrlEscapers.urlFormParameterEscaper().escape(value));
        }
        while (parts.hasNext()) {
          stringBuilder.append("&");
          Map.Entry<String, Object> e = parts.next();
          stringBuilder.append(UrlEscapers.urlFormParameterEscaper().escape(e.getKey()));
          String v = e.getValue().toString();
          if (v != null && v.length() > 0) {
            stringBuilder.append("=");
            stringBuilder.append(UrlEscapers.urlFormParameterEscaper().escape(v));
          }
        }
      }
    }
  }


  @Override
  public String toString() {
    StringBuilder uri = new StringBuilder(protocol).append("://").append(host);
    if (port > -1) {
      uri.append(":").append(port);
    }
    appendPathString(uri);
    if (hasTrailingSlash) {
      uri.append("/");
    }
    appendQueryString(uri);

    if (fragment != null) {
      uri.append("#").append(fragment);
    }

    return uri.toString();
  }
}
