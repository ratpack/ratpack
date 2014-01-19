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

package ratpack.parse;

import static ratpack.util.internal.Types.findImplParameterTypeAtIndex;

/**
 * A convenience base for {@link ratpack.parse.NoOptParser} implementations.
 * <p>
 * The following is an example of an implementation that parses to an {@code Integer}.
 * <pre class="tested">
 * import ratpack.parse.NoOptParse;
 * import ratpack.parse.NoOptParserSupport;
 * import ratpack.http.TypedData;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 *
 * public class IntParser extends NoOptParserSupport<Integer> {
 *   public IntParser() {
 *     super("text/plain");
 *   }
 *
 *   public Integer parse(Context context, TypedData body, NoOptParse&lt;Integer&gt; parse) {
 *     return Integer.valueOf(body.getText());
 *   }
 * }
 *
 * public class ExampleHandler implements Handler {
 *   public void handle(Context context) {
 *     // assuming IntParser has been registered upstream
 *     Integer integer = context.parse(Integer.class);
 *     // …
 *   }
 * }
 * </pre>
 *
 * @param <T> the type of object to parse to
 */
public abstract class NoOptParserSupport<T> implements NoOptParser<T> {

  private final Class<NoOptParse<T>> parseType;
  private final Class<T> parsedType;
  private final String contentType;

  /**
   * Constructor.
   *
   * @param contentType the type of request this parser can handle
   */
  protected NoOptParserSupport(String contentType) {
    this.contentType = contentType;
    this.parsedType = findImplParameterTypeAtIndex(getClass(), NoOptParserSupport.class, 0);
    @SuppressWarnings("unchecked") Class<NoOptParse<T>> cast = (Class<NoOptParse<T>>) NoOptParse.to(parsedType).getClass();
    this.parseType = cast;
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
  public Class<NoOptParse<T>> getParseType() {
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
