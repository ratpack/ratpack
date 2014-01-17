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
import com.fasterxml.jackson.databind.ObjectReader;
import ratpack.handling.Context;
import ratpack.http.TypedData;
import ratpack.jackson.JsonParse;
import ratpack.parse.ParserSupport;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

import static ratpack.util.ExceptionUtils.uncheck;

public class JsonNodeParser extends ParserSupport<JsonNode, JsonParse<JsonNode>> {

  private final ObjectReader objectReader;

  @Inject
  public JsonNodeParser(ObjectReader objectReader) {
    this.objectReader = objectReader;
  }

  @Override
  public String getContentType() {
    return "application/json";
  }

  @Override
  public JsonNode parse(Context context, TypedData body, JsonParse<JsonNode> parse) {
    try {
      InputStream inputStream = body.getInputStream();
      return getObjectReader(parse).readTree(inputStream);
    } catch (IOException e) {
      throw uncheck(e);
    }
  }

  private ObjectReader getObjectReader(JsonParse<JsonNode> parse) {
    return parse.getObjectReader() == null ? objectReader : parse.getObjectReader();
  }

}
