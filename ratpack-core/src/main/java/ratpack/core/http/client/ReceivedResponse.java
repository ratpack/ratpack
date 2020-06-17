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

package ratpack.core.http.client;

import ratpack.core.http.Response;
import ratpack.core.http.TypedData;

public interface ReceivedResponse extends HttpResponse {

  /**
   *
   * @return The {@link TypedData} that represents the body.
   */
  TypedData getBody();

  void forwardTo(Response response);

}
