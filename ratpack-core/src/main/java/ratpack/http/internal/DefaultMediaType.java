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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.http.MediaType;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import static ratpack.util.Exceptions.toException;
import static ratpack.util.Exceptions.uncheck;

public class DefaultMediaType implements MediaType {

  public static final String CHARSET_KEY = "charset";

  private final String type;
  private final ImmutableListMultimap<String, String> params;
  private final String string;

  private static final int CACHE_SIZE = 200;

  private static final Cache<String, MediaType> CACHE = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();

  public static MediaType get(final String contentType) {
    String contentType1 = contentType;
    if (contentType1 == null) {
      contentType1 = "";
    } else {
      contentType1 = contentType1.trim();
    }

    final String finalContentType = contentType1;
    try {
      return CACHE.get(contentType1, () -> new DefaultMediaType(finalContentType));
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  public DefaultMediaType(String value) {
    if (value == null || value.trim().length() == 0) {
      type = null;
      params = ImmutableListMultimap.of();
      string = "";
    } else {
      com.google.common.net.MediaType mediaType = com.google.common.net.MediaType.parse(value.trim());
      if (mediaType != null && mediaType.type() != null) {
        if (mediaType.subtype() != null) {
          type = mediaType.type() + "/" + mediaType.subtype();
        } else {
          type = mediaType.type();
        }
        params = mediaType.parameters();
        string = mediaType.toString();
      } else {
        type = null;
        params = ImmutableListMultimap.of();
        string ="";
      }
    }
  }

  public String getType() {
    return type;
  }

  public ImmutableListMultimap<String, String> getParams() {
    return params;
  }

  public String getCharset() {
    return getCharset(null);
  }

  public String getCharset(String defaultCharset) {
    ImmutableList<String> charsetValues = params.get(CHARSET_KEY);
    switch (charsetValues.size()) {
      case 0:
        return defaultCharset;
      case 1:
        return Charset.forName(charsetValues.get(0)).toString();
      default:
        throw new IllegalStateException("Multiple charset values defined: " + charsetValues);
    }
  }

  public boolean isText() {
    return getType() != null && getType().startsWith("text/");
  }

  public boolean isJson() {
    return getType() != null && getType().equals(APPLICATION_JSON);
  }

  public boolean isForm() {
    return getType() != null && getType().equals(APPLICATION_FORM);
  }

  @Override
  public boolean isHtml() {
    return getType() != null && getType().equals(TEXT_HTML);
  }

  public boolean isEmpty() {
    return getType() == null;
  }

  @Override
  public String toString() {
    return string;
  }
}
