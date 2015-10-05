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

package ratpack.jackson.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.jackson.JsonParseOpts;

import java.util.Optional;

public class DefaultJsonParseOpts implements JsonParseOpts {

  public static final JsonParseOpts INSTANCE = new DefaultJsonParseOpts(null);

  private final Optional<ObjectMapper> objectMapper;

  public DefaultJsonParseOpts(ObjectMapper objectMapper) {
    this.objectMapper = Optional.ofNullable(objectMapper);
  }

  @Override
  public Optional<ObjectMapper> getObjectMapper() {
    return objectMapper;
  }

  @Override
  public String toString() {
    return "JsonParseOpts{objectMapper=" + objectMapper.orElse(null) + '}';
  }
}
