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

package ratpack.util.internal;

import com.google.common.collect.ImmutableMap;

public class ProtocolUtil {
  public static final String HTTP_SCHEME = "http";
  public static final String HTTPS_SCHEME = "https";

  public static final int DEFAULT_HTTP_PORT = 80;
  public static final int DEFAULT_HTTPS_PORT = 443;

  private static final ImmutableMap<String, Integer> DEFAULT_PORT_BY_SCHEME = ImmutableMap.<String, Integer>builder().put(HTTP_SCHEME, DEFAULT_HTTP_PORT).put(HTTPS_SCHEME, DEFAULT_HTTPS_PORT).build();

  public static boolean isDefaultPortForScheme(int port, String scheme) {
    return DEFAULT_PORT_BY_SCHEME.get(scheme) == port;
  }
}
