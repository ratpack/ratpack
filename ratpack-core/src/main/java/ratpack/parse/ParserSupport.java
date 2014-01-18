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

import static ratpack.util.internal.Types.findImplParameterTypeAtIndex;

/**
 * A convenience superclass of {@link Parser} that implements the type methods based on the implementation.
 * <p>
 * Specializations only need to implement the {@link Parser#parse(ratpack.handling.Context, ratpack.http.TypedData, Parse)} method.
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.http.TypedData;
 * import ratpack.parse.ParseSupport;
 * import ratpack.parse.ParserSupport;
 * import ratpack.parse.ParseException;
 *
 * public class MaxLengthStringParse extends ParseSupport&lt;String&gt; {
 *   private int maxLength;
 *
 *   public MaxLengthStringParse(int maxLength) {
 *     this.maxLength = maxLength;
 *   }
 *
 *   public int getMaxLength() {
 *     return maxLength;
 *   }
 * }
 *
 * // A parser for this parser type…
 *
 * public class MaxLengthStringParser extends ParserSupport<String, MaxLengthStringParse> {
 *   public MaxLengthStringParser() {
 *     super("text/plain");
 *   }
 *
 *   String parse(Context context, TypedData requestBody, MaxLengthStringParse parse) throws UnsupportedEncodingException {
 *     String rawString = requestBody.getText();
 *     if (rawString.length() < parse.getMaxLength()) {
 *       return rawString;
 *     } else {
 *       return rawString.substring(0, parse.getMaxLength());
 *     }
 *   }
 * }
 *
 * // Assuming the parser above has been registered upstream…
 *
 * public class ExampleHandler implements Handler {
 *   public void handle(Context context) throws ParseException {
 *     String string = context.parse(new MaxLengthStringParse(20));
 *     // …
 *   }
 * }
 * </pre>
 *
 * @param <T> the type of object this parser parses to
 * @param <P> the type of parse object this parser can handle
 */
abstract public class ParserSupport<T, P extends Parse<T>> implements Parser<T, P> {

  private final Class<P> parseType;
  private final Class<T> parsedType;
  private final String contentType;

  /**
   * Constructor.
   *
   * @param contentType the type of request this parser can handle
   */
  protected ParserSupport(String contentType) {
    this.contentType = contentType;
    this.parsedType = findImplParameterTypeAtIndex(getClass(), ParserSupport.class, 0);
    this.parseType = findImplParameterTypeAtIndex(getClass(), ParserSupport.class, 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getContentType() {
    return contentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<P> getParseType() {
    return parseType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getParsedType() {
    return parsedType;
  }

}
