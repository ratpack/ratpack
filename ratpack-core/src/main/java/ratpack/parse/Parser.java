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

import ratpack.handling.Context;
import ratpack.http.TypedData;

/**
 * A parser is able to deserialize the body of a request into an object representation.
 * <p>
 * Parses power the {@link Context#parse(Parse)} mechanism.
 * <p>
 * A parser works with requests of a given content type (as advertised by {@link #getContentType()})
 * and with a particular type of {@link Parse} object (as advertised by {@link #getParseType()}),
 * of a particular final parsed type (as advertised by {@link #getParsedType()}).
 * <p>
 * The {@link ParserSupport} class is a convenient base, the documentation
 * of which contains implementation examples.
 *
 * @param <T> The type that this parser deserializes to
 * @see Parse
 * @see ParserSupport
 * @see Context#parse(Parse)
 */
public interface Parser<O> {

  /**
   * The content type that this parser knows how to deserialize.
   *
   * @return The content type that this parser knows how to deserialize.
   */
  String getContentType();

  Class<O> getOptsType();

  /**
   * Deserializes the request body of the context into an object.
   *
   * @param context The context to deserialize
   * @param requestBody The request body to deserialize
   * @param parse The description of how to parse the request body
   * @return The object representation of the request body
   * @throws Exception if an error occurs parsing the request
   */
  <T> T parse(Context context, TypedData requestBody, O options, Class<T> type) throws Exception;

}
