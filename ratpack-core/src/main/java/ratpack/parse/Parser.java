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
 * The {@link ratpack.parse.ParserSupport} class is a convenient base for implementations.
 *
 * @param <T> The type that this parser deserializes to
 * @param <P> The type of the “parse object” which describes how to parse the request
 */
public interface Parser<T, P extends Parse<T>> {

  /**
   * The content type that this parser knows how to deserialize.
   *
   * @return The content type that this parser knows how to deserialize.
   */
  String getContentType();

  /**
   * The type of the {@link Parse} object for this parser.
   *
   * @return The type of the {@link Parse} object for this parser.
   */
  Class<P> getParseType();

  /**
   * The type that this parser can deserialize to.
   *
   * @return The type that this parser can deserialize to.
   */
  Class<T> getParsedType();

  /**
   * Deserializes the request body of the context into an object.
   *
   * @param context The context to deserialize
   * @param requestBody The request body to deserialize
   * @param parse The description of how to parse the request body
   * @return The object representation of the request body
   */
  T parse(Context context, TypedData requestBody, P parse);


}
