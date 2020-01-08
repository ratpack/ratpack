/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics;

import io.micrometer.core.instrument.Tag;
import ratpack.http.client.HttpResponse;
import ratpack.http.client.RequestSpec;

public class ClientRequestTags {
  private static final Tag STATUS_UNKNOWN = Tag.of("status", "UNKNOWN");

  private static final Tag METHOD_UNKNOWN = Tag.of("method", "UNKNOWN");

  /**
   * Creates a {@code method} tag based on the {@link RequestSpec#getMethod()
   * method} of the given {@code request}.
   * @param request the request
   * @return the method tag whose value is a capitalized method (e.g. GET).
   */
  public static Tag method(RequestSpec request) {
    return (request != null) ? Tag.of("method", request.getMethod().getName()) : METHOD_UNKNOWN;
  }

  /**
   * Creates a {@code status} tag based on the status of the given {@code response}.
   * @param response the HTTP response
   * @return the status tag derived from the status of the response
   */
  public static Tag status(HttpResponse response) {
    return (response != null) ? Tag.of("status", Integer.toString(response.getStatus().getCode())) : STATUS_UNKNOWN;
  }

  /**
   * Creates an {@code outcome} tag based on the status of the given {@code response}.
   * @param response the HTTP response
   * @return the outcome tag derived from the status of the response
   */
  public static Tag outcome(HttpResponse response) {
    Outcome outcome = (response != null) ? Outcome.forStatus(response.getStatus().getCode()) : Outcome.UNKNOWN;
    return outcome.asTag();
  }
}
