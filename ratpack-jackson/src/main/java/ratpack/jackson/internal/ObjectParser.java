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

package ratpack.jackson.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import ratpack.handling.Context;

import ratpack.http.TypedData;
import ratpack.parse.ParserSupport;

import java.io.IOException;
import java.io.InputStream;

import static ratpack.util.ExceptionUtils.uncheck;

public class ObjectParser extends ParserSupport<Object, ObjectParse> {

  private final ObjectMapper objectMapper;

  @Inject
  public ObjectParser(ObjectMapper objectMapper) {
    super("application/json");
    this.objectMapper = objectMapper;
  }

  @Override
  public Object parse(Context context, TypedData body, ObjectParse parse) {
    try {
      InputStream inputStream = body.getInputStream();
      return objectMapper.readValue(inputStream, parse.getType());
    }
    catch (IOException e) {
      throw uncheck(e);
    }
  }



}
