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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.jackson.JsonParseOpts;
import ratpack.parse.ParserSupport;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

import static ratpack.util.ExceptionUtils.uncheck;

public class JsonParser extends ParserSupport<JsonParseOpts> {

  private final ObjectMapper objectMapper;

  @Inject
  public JsonParser(ObjectMapper objectMapper) {
    super("application/json");
    this.objectMapper = objectMapper;
  }

  @Override
  public <T> T parse(Context context, TypedData body, JsonParseOpts opts, Class<T> type) {
    ObjectMapper objectMapper = getObjectMapper(opts);
    try {
      InputStream inputStream = body.getInputStream();
      if (type.equals(JsonNode.class)) {
        return type.cast(objectMapper.readTree(inputStream));
      } else {
        return objectMapper.readValue(inputStream, type);
      }
    } catch (IOException e) {
      throw uncheck(e);
    }
  }

  private ObjectMapper getObjectMapper(JsonParseOpts opts) {
    return opts.getObjectMapper() == null ? objectMapper : opts.getObjectMapper();
  }

}
