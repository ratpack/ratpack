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

import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.http.TypedData;

/**
 * A convenience base for parsers that don't require options.
 * <p>
 * The following is an example of an implementation that parses to an {@code Integer}.
 * <pre class="java">
 * import ratpack.parse.NullParseOpts;
 * import ratpack.parse.NoOptParserSupport;
 * import ratpack.http.TypedData;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.util.Types;
 * import ratpack.func.Action;
 * import ratpack.registry.RegistrySpec;
 *
 * import com.google.common.reflect.TypeToken;
 *
 * import ratpack.test.UnitTest;
 * import ratpack.test.handling.HandlingResult;
 * import ratpack.test.handling.RequestFixture;
 *
 * public class Example {
 *   public static class IntParser extends NoOptParserSupport {
 *     public IntParser() {
 *       super("text/plain");
 *     }
 *
 *     public &lt;T&gt; T parse(Context context, TypedData body, TypeToken&lt;T&gt; type) {
 *       if (type.getRawType().equals(Integer.class)) {
 *         return Types.cast(Integer.valueOf(body.getText()));
 *       } else {
 *         return null;
 *       }
 *     }
 *   }
 *
 *   public static class ExampleHandler implements Handler {
 *     public void handle(Context context) {
 *       Integer integer = context.parse(Integer.class);
 *       context.render(integer.toString());
 *     }
 *   }
 *
 *   // unit test
 *   public static void main(String[] args) {
 *     HandlingResult result = UnitTest.handle(new ExampleHandler(), new Action&lt;RequestFixture&gt;() {
 *       public void execute(RequestFixture fixture) throws Exception {
 *         fixture
 *           .body("10", "text/plain")
 *           .registry(new Action&lt;RegistrySpec&gt;() {
 *             public void execute(RegistrySpec registry) {
 *               registry.add(new IntParser());
 *             }
 *           });
 *       }
 *     });
 *
 *     assert result.rendered(String.class).equals("10");
 *   }
 *
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
   * Delegates to {@link #parse(ratpack.handling.Context, ratpack.http.TypedData, TypeToken)}, discarding the {@code} opts object of the given {@code parse}.
   *
   * @param context The context to deserialize
   * @param requestBody The request body to deserialize
   * @param parse The description of how to parse the request body
   * @param <T> the type of object to construct from the request body
   * @return the result of calling {@link #parse(ratpack.handling.Context, ratpack.http.TypedData, TypeToken)}
   * @throws Exception any exception thrown by {@link #parse(ratpack.handling.Context, ratpack.http.TypedData, TypeToken)}
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
  abstract protected <T> T parse(Context context, TypedData requestBody, TypeToken<T> type) throws Exception;

}
