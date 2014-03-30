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
 * A convenience superclass for {@link Parser} implementations.
 * <p>
 * Specializations only need to implement the {@link Parser#parse(ratpack.handling.Context, ratpack.http.TypedData, Parse)} method.
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.http.TypedData;
 * import ratpack.parse.Parse;
 * import ratpack.parse.ParserSupport;
 * import ratpack.parse.ParseException;
 *
 * public class StringParseOpts {
 *   private int maxLength;
 *
 *   public StringParseOpts(int maxLength) {
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
 * public class MaxLengthStringParser extends ParserSupport<StringParseOpts> {
 *   public MaxLengthStringParser() {
 *     super("text/plain");
 *   }
 *
 *   public &lt;T&gt; T parse(Context context, TypedData requestBody, Parse&lt;T, StringParseOpts&gt; parse) throws UnsupportedEncodingException {
 *     if (!parse.getType().equals(String.class)) {
 *       return null;
 *     }
 *
 *     String rawString = requestBody.getText();
 *     if (rawString.length() < parse.getOpts().getMaxLength()) {
 *       return parse.getType().cast(rawString);
 *     } else {
 *       return parse.getType().cast(rawString.substring(0, parse.getOpts().getMaxLength()));
 *     }
 *   }
 * }
 *
 * // Assuming the parser above has been registered upstream…
 *
 * public class ExampleHandler implements Handler {
 *   public void handle(Context context) throws ParseException {
 *     String string = context.parse(String.class, new StringParseOpts(20));
 *     // …
 *   }
 * }
 * </pre>
 *
 * @see NoOptParserSupport
 * @param <O> the type of option object this parser accepts
 */
abstract public class ParserSupport<O> implements Parser<O> {

  private final Class<O> optsType;
  private final String contentType;

  /**
   * Constructor.
   *
   * @param contentType the type of request this parser can handle
   */
  protected ParserSupport(String contentType) {
    this.contentType = contentType;
    this.optsType = findImplParameterTypeAtIndex(getClass(), ParserSupport.class, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getContentType() {
    return contentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Class<O> getOptsType() {
    return optsType;
  }

}
