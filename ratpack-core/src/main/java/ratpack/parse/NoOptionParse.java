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

/**
 * A generic parse implementation that can be used when parsers do not need any extra information from parse objects other than type.
 * <p>
 * For example, a string to int parser could use this as its parse type.
 * <pre class="tested">
 * import ratpack.parse.ParserSupport;
 * import ratpack.parse.NoOptionParse;
 * import ratpack.http.TypedData;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import static ratpack.parse.NoOptionParse.to;
 *
 * public class IntParser extends ParserSupport<Integer, NoOptionParse<Integer>> {
 *   public String getContentType() {
 *     return "text/plain";
 *   }
 *
 *   public Integer parse(Context context, TypedData body, NoOptionParse<Integer> parse) {
 *      return Integer.valueOf(body.getText());
 *   }
 * }
 *
 * public class ExampleHandler implements Handler {
 *   public void handle(Context context) {
 *     // assuming IntParser has been registered upstream
 *     Integer integer = context.parse(to(Integer));
 *     // …
 *   }
 * }
 * </pre>
 * @param <T> The type of object to parse to
 */
public class NoOptionParse<T> implements Parse<T> {

  private final Class<T> type;

  /**
   * Constructor.
   *
   * @param type The type of object to parse to
   */
  public NoOptionParse(Class<T> type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getType() {
    return type;
  }

  /**
   * Convenience factory for implementations.
   *
   * @param type The type to parse to
   * @param <T> The type to parse to
   * @return An no option parser for the given type
   */
  public static <T> NoOptionParse<T> to(Class<T> type) {
    return new NoOptionParse<>(type);
  }

}
