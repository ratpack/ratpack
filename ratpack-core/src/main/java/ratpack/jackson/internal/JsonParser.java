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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.jackson.JsonParseOpts;
import ratpack.parse.Parse;
import ratpack.parse.ParserSupport;

import java.io.IOException;
import java.io.InputStream;

import static ratpack.util.Types.cast;

public class JsonParser extends ParserSupport<JsonParseOpts> {

  private static final TypeToken<JsonNode> JSON_NODE_TYPE = TypeToken.of(JsonNode.class);

  private final ObjectMapper objectMapper;

  public JsonParser(ObjectMapper objectMapper) {
    super("application/json");
    this.objectMapper = objectMapper;
  }

  @Override
  public <T> T parse(Context context, TypedData body, Parse<T, JsonParseOpts> parse) throws IOException {
    JsonParseOpts opts = parse.getOpts();
    TypeToken<T> type = parse.getType();

    ObjectMapper objectMapper = getObjectMapper(opts);
    InputStream inputStream = body.getInputStream();
    if (type.equals(JSON_NODE_TYPE)) {
      return cast(objectMapper.readTree(inputStream));
    } else {
      return objectMapper.readValue(inputStream, toJavaType(type, objectMapper));
    }
  }

  private <T> JavaType toJavaType(TypeToken<T> type, ObjectMapper objectMapper) {
    return objectMapper.getTypeFactory().constructType(type.getType());
  }

  private ObjectMapper getObjectMapper(JsonParseOpts opts) {
    return opts.getObjectMapper() == null ? objectMapper : opts.getObjectMapper();
  }

}
