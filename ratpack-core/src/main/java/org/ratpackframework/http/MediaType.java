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

package org.ratpackframework.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MediaType {

  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_FORM = "application/x-www-form-urlencoded";

  private String base;
  protected final Map<String, String> params;

  public MediaType(String base) {
    this(base, "ISO-8859-1");
  }

  /**
   * Parses the media type from its string representation (including params).
   */
  public MediaType(String headerValue, String defaultCharset) {
    // TODO: separate this out into a parser and interface.
    if (headerValue == null) {
      base = null;
      params = Collections.emptyMap();
    } else {
      headerValue = headerValue.trim();
      if (headerValue.isEmpty()) {
        base = null;
        params = Collections.emptyMap();
      } else {
        params = new LinkedHashMap<>();
        String[] parts = headerValue.split(";");
        base = parts[0].toLowerCase();
        if (parts.length > 1) {
          for (int i = 1; i < parts.length; ++i) {
            String part = parts[i].trim();
            if (part.contains("=")) {
              String[] keyValue = part.split("=", 2);
              params.put(keyValue[0].toLowerCase(), keyValue[1]);
            } else {
              params.put(part.toLowerCase(), null);
            }
          }
        }

        if (isText() && !params.containsKey("charset")) {
          params.put("charset", defaultCharset);
        }
      }
    }
  }

  /**
   * Given a mime type of "application/json;charset=utf-8", returns "application/json".
   *
   * May be null to represent no content type.
   *
   * @return The mime type "base"
   */
  public String getBase() {
    return base;
  }

  /**
   * Returns an unmodifiable view of the parameters of the mime type.
   *
   * Given a mime type of "application/json;charset=utf-8", returns "[charset=utf-8]".
   * May be empty, never null.
   * <p>
   * All param names have been lowercased.
   * It is invalid to have {@code getBase()} return null and this not return an empty map.
   *
   * @return the media type params.
   */
  public Map<String, String> getParams() {
    return Collections.unmodifiableMap(params);
  }

  public String getCharset() {
    return params.get("charset");
  }

  public boolean isText() {
    return !isEmpty() && getBase().startsWith("text/");
  }

  /**
   * Is the base {@value #APPLICATION_JSON}?
   */
  public boolean isJson() {
    return !isEmpty() && getBase().equals(APPLICATION_JSON);
  }

  /**
   * Is the base {@value #APPLICATION_FORM}?
   */
  public boolean isForm() {
    return !isEmpty() && getBase().equals(APPLICATION_FORM);
  }

  /**
   * Is this an empty value? (i.e. {@link #getBase()} == null)
   */
  public boolean isEmpty() {
    return getBase() == null;
  }

  /**
   * The proper string representation of a media type (e.g. for a Content-Type header)
   */
  @Override
  public String toString() {
    if (isEmpty()) {
      return "";
    } else {
      StringBuilder s = new StringBuilder(getBase());
      for (Map.Entry<String, String> param : getParams().entrySet()) {
        s.append(";").append(param.getKey());
        if (param.getValue() != null) {
          s.append("=").append(param.getValue());
        }
      }
      return s.toString();
    }
  }
}
