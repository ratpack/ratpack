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

import com.google.common.reflect.TypeToken;

/**
 * A convenience superclass for {@link Parser} implementations.
 * <p>
 * Specializations only need to implement the {@link Parser#parse(ratpack.handling.Context, ratpack.exec.Promise, Parse)} method.
 * <pre class="java">{@code
 * import ratpack.exec.Promise;
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.http.TypedData;
 * import ratpack.parse.Parse;
 * import ratpack.parse.ParserSupport;
 * import ratpack.parse.ParseException;
 * import ratpack.util.Types;
 *
 * import java.io.UnsupportedEncodingException;
 *
 * import ratpack.test.handling.HandlingResult;
 * import ratpack.test.handling.RequestFixture;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   // The parse options
 *   public static class StringParseOpts {
 *     private int maxLength;
 *
 *     public StringParseOpts(int maxLength) {
 *       this.maxLength = maxLength;
 *     }
 *
 *     public int getMaxLength() {
 *       return maxLength;
 *     }
 *   }
 *
 *   // A parser for this type
 *   public static class MaxLengthStringParser extends ParserSupport<StringParseOpts> {
 *     public MaxLengthStringParser() {
 *       super("text/plain");
 *     }
 *
 *     public <T> Promise<T> parse(Context context, Promise<TypedData> requestBody, Parse<T, StringParseOpts> parse) throws UnsupportedEncodingException {
 *       return requestBody.map(b -> {
 *         if (!parse.getType().getRawType().equals(String.class)) {
 *           return null;
 *         }
 *
 *         String rawString = b.getText();
 *         if (rawString.length() < parse.getOpts().getMaxLength()) {
 *           return Types.cast(rawString);
 *         } else {
 *           return Types.cast(rawString.substring(0, parse.getOpts().getMaxLength()));
 *         }
 *       });
 *     }
 *   }
 *
 *   public static class ToUpperCaseHandler implements Handler {
 *     public void handle(Context context) throws Exception {
 *       context.parse(String.class, new StringParseOpts(5)).then(string -> context.render(string));
 *     }
 *   }
 *
 *   // unit test
 *   public static void main(String[] args) throws Exception {
 *     HandlingResult result = RequestFixture.handle(new ToUpperCaseHandler(), fixture ->
 *         fixture
 *           .body("123456", "text/plain")
 *           .registry(registry -> registry.add(new MaxLengthStringParser()))
 *     );
 *
 *     assertEquals("12345", result.rendered(String.class));
 *   }
 * }
 * }</pre>
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

    TypeToken<O> typeToken = new TypeToken<O>(getClass()) {
    };

    if (typeToken.getType() instanceof Class) {
      @SuppressWarnings("unchecked") Class<O> rawType = (Class<O>) typeToken.getRawType();
      this.optsType = rawType;
    } else {
      throw new IllegalArgumentException("Type parameter O of ParserSupport must be a Class");
    }
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
