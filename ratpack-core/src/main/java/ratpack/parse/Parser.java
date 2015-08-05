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

package ratpack.parse;

import ratpack.api.Nullable;
import ratpack.handling.Context;
import ratpack.http.TypedData;

/**
 * A parser converts a request body into an object.
 * <p>
 * Parsers power the {@link Context#parse(Parse)} mechanism.
 * <p>
 * The {@link ParserSupport} class is a convenient base; the documentation of which contains implementation examples.
 *
 * @param <O> the type of option object this parser accepts
 * @see Parse
 * @see ParserSupport
 * @see NoOptParserSupport
 * @see Context#parse(Parse)
 */
public interface Parser<O> {

  /**
   * The type of option object that this parser accepts.
   *
   * @see ParserSupport
   * @return the type of option object that this parser accepts
   */
  Class<O> getOptsType();

  /**
   * Deserializes the request body of the context into an object.
   *
   * @param context The context to deserialize
   * @param requestBody The request body to deserialize
   * @param parse The description of how to parse the request body
   * @param <T> the type of object to construct from the request body
   * @return The object representation of the request body, or {@code null} if this parser cannot parse to the requested type
   * @throws Exception if an error occurs parsing the request
   */
  @Nullable
  <T> T parse(Context context, TypedData requestBody, Parse<T, O> parse) throws Exception;

}
