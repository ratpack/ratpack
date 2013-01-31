/*
 * Copyright 2012 the original author or authors.
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

  public static final String TEXT_HTML = "text/html";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_FORM = "application/x-www-form-urlencoded";

  private final String base;
  private final Map<String, String> params;

  public MediaType(String headerValue) {
    if (headerValue == null) {
      base = null;
      params = Collections.emptyMap();
    } else {
      headerValue = headerValue.trim();
      if (headerValue.isEmpty()) {
        base = null;
        params = Collections.emptyMap();
      } else {
        Map<String, String> mutableParams = new LinkedHashMap<>();
        String[] parts = headerValue.split(";");
        base = parts[0].toLowerCase();
        if (parts.length > 1) {
          for (int i = 1; i < parts.length; ++i) {
            String part = parts[i].trim();
            if (part.contains("=")) {
              String[] keyValue = part.split("=", 2);
              mutableParams.put(keyValue[0].toLowerCase(), keyValue[1]);
            } else {
              mutableParams.put(part.toLowerCase(), null);
            }
          }
        }
        params = Collections.unmodifiableMap(mutableParams);
      }
    }
  }

  public String getBase() {
    return base;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public String getCharset() {
    return params.containsKey("charset") ? params.get("charset") : "ISO-8859-1";
  }

  public boolean isJson() {
    return !isEmpty() && getBase().equals(APPLICATION_JSON);
  }

  public boolean isForm() {
    return !isEmpty() && getBase().equals(APPLICATION_FORM);
  }

  public boolean isEmpty() {
    return getBase() == null;
  }
}
