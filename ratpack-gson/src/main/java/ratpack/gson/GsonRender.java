/*
 * Copyright 2018 the original author or authors.
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

package ratpack.gson;

import ratpack.api.Nullable;

/**
 * A {@link ratpack.handling.Context#render renderable object wrapper} for rendering as JSON.
 *
 * @see Gson#json
 * @since 1.6
 */
public interface GsonRender {

  /**
   * The underlying object to be rendered.
   *
   * @return The underlying object to be rendered.
   */
  Object getObject();

  /**
   * The {@link com.google.gson.Gson} instance to use when serializing the object to JSON.
   * If {@code null} then an instance will be obtained from the registry or a default instance will be created.
   * @return the Gson instance used in serialization.
   */
  @Nullable
  com.google.gson.Gson getGson();

}
