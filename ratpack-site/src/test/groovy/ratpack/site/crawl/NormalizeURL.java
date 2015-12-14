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

package ratpack.site.crawl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

/**
 * - Covert the scheme and host to lowercase (done by java.net.URL)
 * - Normalize the path (done by java.net.URI)
 * - Add the port githubNumber.
 * - Remove the fragment (the part after the #).
 * - Remove trailing slash.
 * - Sort the query string params.
 * - Remove some query string params like "utm_*" and "*session*".
 */
//CHECKSTYLE:OFF
public class NormalizeURL {
  private final static Logger LOGGER = LoggerFactory.getLogger(NormalizeURL.class);

  public static String normalize(final String taintedURL) throws MalformedURLException {
    final URL url;
    try {
      url = new URI(taintedURL).normalize().toURL();
    } catch (URISyntaxException e) {
      throw new MalformedURLException(e.getMessage());
    }

    final String path = url.getPath().replace("/$", "");
    final SortedMap<String, String> params = createParameterMap(url.getQuery());
    final int port = url.getPort();
    final String queryString;

    if (params != null) {
      // Some params are only relevant for user tracking, so remove the most commons ones.
      for (Iterator<String> i = params.keySet().iterator(); i.hasNext();) {
        final String key = i.next();
        if (key.startsWith("utm_") || key.contains("session")) {
          i.remove();
        }
      }
      queryString = "?" + canonicalize(params);
    } else {
      queryString = "";
    }

    String value = url.getProtocol() + "://" + url.getHost()
      + (port != -1 && port != 80 ? ":" + port : "")
      + path + queryString;

    if (url.getRef() != null) {
      value = value + "#" + url.getRef();
    }

    return value;
  }

  /**
   * Takes a query string, separates the constituent name-value pairs, and
   * stores them in a SortedMap ordered by lexicographical order.
   *
   * @return Null if there is no query string.
   */
  private static SortedMap<String, String> createParameterMap(final String queryString) {
    if (queryString == null || queryString.isEmpty()) {
      return null;
    }

    final String[] pairs = queryString.split("&");
    final Map<String, String> params = new HashMap<String, String>(pairs.length);

    for (final String pair : pairs) {
      if (pair.length() < 1) {
        continue;
      }

      String[] tokens = pair.split("=", 2);
      for (int j = 0; j < tokens.length; j++) {
        try {
          tokens[j] = URLDecoder.decode(tokens[j], "UTF-8");
        } catch (UnsupportedEncodingException ex) {
          LOGGER.error("", ex);
        }
      }
      switch (tokens.length) {
        case 1: {
          if (pair.charAt(0) == '=') {
            params.put("", tokens[0]);
          } else {
            params.put(tokens[0], "");
          }
          break;
        }
        case 2: {
          params.put(tokens[0], tokens[1]);
          break;
        }
      }
    }

    return new TreeMap<>(params);
  }

  /**
   * Canonicalize the query string.
   *
   * @param sortedParamMap Parameter name-value pairs in lexicographical order.
   * @return Canonical form of query string.
   */
  private static String canonicalize(final SortedMap<String, String> sortedParamMap) {
    if (sortedParamMap == null || sortedParamMap.isEmpty()) {
      return "";
    }

    final StringBuffer sb = new StringBuffer(350);
    final Iterator<Map.Entry<String, String>> iter = sortedParamMap.entrySet().iterator();

    while (iter.hasNext()) {
      final Map.Entry<String, String> pair = iter.next();
      sb.append(percentEncodeRfc3986(pair.getKey()));
      sb.append('=');
      sb.append(percentEncodeRfc3986(pair.getValue()));
      if (iter.hasNext()) {
        sb.append('&');
      }
    }

    return sb.toString();
  }

  /**
   * Percent-encode values according the RFC 3986. The built-in Java URLEncoder does not encode
   * according to the RFC, so we make the extra replacements.
   *
   * @param string Decoded string.
   * @return Encoded string per RFC 3986.
   */
  private static String percentEncodeRfc3986(final String string) {
    try {
      return URLEncoder.encode(string, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    } catch (UnsupportedEncodingException e) {
      return string;
    }
  }
}
//CHECKSTYLE:ON
