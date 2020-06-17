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

package ratpack.core.jackson.internal;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import ratpack.core.handling.Context;
import ratpack.core.http.TypedData;
import ratpack.core.jackson.JsonParseOpts;
import ratpack.core.parse.Parse;
import ratpack.core.parse.Parser;
import ratpack.core.parse.ParserSupport;
import ratpack.exec.util.Types;

import java.io.IOException;
import java.io.InputStream;

import static ratpack.exec.util.Types.cast;

public class JsonParser extends ParserSupport<JsonParseOpts> {

  private static final TypeToken<JsonNode> JSON_NODE_TYPE = Types.token(JsonNode.class);

  public static final TypeToken<Parser<JsonParseOpts>> TYPE = Types.intern(new TypeToken<Parser<JsonParseOpts>>() {});
  public static final Parser<JsonParseOpts> INSTANCE = new JsonParser();

  private JsonParser() {
  }

  @Override
  public <T> T parse(Context context, TypedData body, Parse<T, JsonParseOpts> parse) throws IOException {
    if (!body.getContentType().isJson()) {
      return null;
    }

    JsonParseOpts opts = parse.getOpts().orElse(DefaultJsonParseOpts.INSTANCE);
    TypeToken<T> type = parse.getType();

    ObjectMapper objectMapper = opts.getObjectMapper().orElseGet(() -> context.get(ObjectMapper.class));
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

  @Override
  public String toString() {
    return getClass().getName() + " (parses 'application/json' and types ending in '+json')";
  }
}
