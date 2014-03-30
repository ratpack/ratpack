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

import ratpack.handling.Context;
import ratpack.http.TypedData;

/**
 * A convenience base for parsers that don't require options.
 * <p>
 * The following is an example of an implementation that parses to an {@code Integer}.
 * <pre class="tested">
 * import ratpack.parse.NullParseOpts;
 * import ratpack.parse.NoOptParserSupport;
 * import ratpack.http.TypedData;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 *
 * public class IntParser extends NoOptParserSupport {
 *   public IntParser() {
 *     super("text/plain");
 *   }
 *
 *   public &lt;T&gt; T parse(Context context, TypedData body, Class&lt;T&gt; type) {
 *     return type.cast(Integer.valueOf(body.getText()));
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
 */
public abstract class NoOptParserSupport extends ParserSupport<NullParseOpts> {

  /**
   * Constructor.
   *
   * @param contentType the type of request this parser can handle
   */
  protected NoOptParserSupport(String contentType) {
    super(contentType);
  }

  /**
   * Delegates to {@link #parse(ratpack.handling.Context, ratpack.http.TypedData, Class)}, discarding the {@code} opts object of the given {@code parse}.
   *
   * @param context The context to deserialize
   * @param requestBody The request body to deserialize
   * @param parse The description of how to parse the request body
   * @param <T> the type of object to construct from the request body
   * @return the result of calling {@link #parse(ratpack.handling.Context, ratpack.http.TypedData, Class)}
   * @throws Exception any exception thrown by {@link #parse(ratpack.handling.Context, ratpack.http.TypedData, Class)}
   */
  @Override
  public final <T> T parse(Context context, TypedData requestBody, Parse<T, NullParseOpts> parse) throws Exception {
    return parse(context, requestBody, parse.getType());
  }

  /**
   * The parser implementation.
   *
   * @param context The context to deserialize
   * @param requestBody The request body to deserialize
   * @param type the type of object to construct from the request body
   * @param <T> the type of object to construct from the request body
   * @return an instance of {@code T} if this parser can construct this type, otherwise {@code null}
   * @throws Exception any exception thrown while parsing
   */
  abstract protected <T> T parse(Context context, TypedData requestBody, Class<T> type) throws Exception;

}
