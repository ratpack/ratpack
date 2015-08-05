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
 * <pre class="java">{@code
 * import com.google.common.reflect.TypeToken;
 * import ratpack.exec.Promise;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.http.TypedData;
 * import ratpack.parse.NoOptParserSupport;
 * import ratpack.test.handling.HandlingResult;
 * import ratpack.test.handling.RequestFixture;
 * import ratpack.util.Types;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static class IntParser extends NoOptParserSupport {
 *     public <T> T parse(Context context, TypedData body, TypeToken<T> type) {
 *       if (type.getRawType().equals(Integer.class)) {
 *         return Types.cast(Integer.valueOf(body.getText()));
 *       } else {
 *         return null;
 *       }
 *     }
 *   }
 *
 *   public static class ExampleHandler implements Handler {
 *     public void handle(Context context) throws Exception {
 *       context.parse(Integer.class).then(integer -> context.render(integer.toString()));
 *     }
 *   }
 *
 *   // unit test
 *   public static void main(String[] args) throws Exception {
 *     HandlingResult result = RequestFixture.handle(new ExampleHandler(),
 *       fixture -> fixture
 *           .body("10", "text/plain")
 *           .registry(registry -> registry.add(new IntParser()))
 *     );
 *
 *     assertEquals("10", result.rendered(String.class));
 *   }
 * }
 * }</pre>
 */
public abstract class NoOptParserSupport extends ParserSupport<Void> {

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
  public final <T> T parse(Context context, TypedData requestBody, Parse<T, Void> parse) throws Exception {
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
