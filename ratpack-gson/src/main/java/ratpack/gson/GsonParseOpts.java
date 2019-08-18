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

import com.google.gson.Gson;

import java.util.Optional;

/**
 * Parsing configuration for Google's Gson library.
 *
 * @since 1.6
 */
public interface GsonParseOpts {

  /**
   * Get the {@link Gson} instance to use when deserializing the JSON.
   * @return the optional Gson instance to use during deserialization.
   */
  Optional<Gson> getGson();
}
