/*
 * Copyright 2018 the original author or authors.
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

package ratpack.gson;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import ratpack.api.Nullable;
import ratpack.func.Function;
import ratpack.gson.internal.DefaultGsonParseOpts;
import ratpack.gson.internal.DefaultGsonRender;
import ratpack.http.ResponseChunks;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.parse.Parse;
import ratpack.registry.Registry;
import ratpack.stream.StreamMapper;
import ratpack.stream.Streams;
import ratpack.stream.WriteStream;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Support for rendering and parsing JSON using Google's Gson library.
 * @since 1.6
 */
public abstract class Gson {

  private Gson() {
  }

  /**
   * Creates a {@link ratpack.handling.Context#render renderable object} to render the given object as JSON.
   * <p>
   * The given object will be converted to JSON using a {@link com.google.gson.Gson} instance obtained from the context registry.
   * <p>
   * See the <a href="#rendering">rendering</a> section for usage examples.
   *
   * @param object the object to render as JSON
   * @return a renderable wrapper for the given object
   */
  public static GsonRender json(Object object) {
    return new DefaultGsonRender(object, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render renderable object} to render the given object as JSON.
   * <p>
   * The given object will be converted to JSON using the given {@link com.google.gson.Gson}.
   * If it is {@code null}, a {@code Gson} instance will be obtained from the context registry.
   * <p>
   * See the <a href="#rendering">rendering</a> section for usage examples.
   *
   * @param object the object to render as JSON
   * @param gson the Gson instance to use to serialize the object to JSON
   * @return a renderable wrapper for the given object
   */
  public static GsonRender json(Object object, @Nullable com.google.gson.Gson gson) {
    return new DefaultGsonRender(object, gson);
  }

  /**
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into a {@link JsonElement}.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using a {@link com.google.gson.Gson} obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @return a parse object
   */
  public static Parse<JsonElement, GsonParseOpts> jsonElement() {
    return jsonElement(null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into a {@link JsonElement}.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using the given {@link com.google.gson.Gson}.
   * If it is {@code null}, a Gson will be obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param gson the Gson instance to use to parse the JSON
   * @return a parse object
   */
  public static Parse<JsonElement, GsonParseOpts> jsonElement(@Nullable com.google.gson.Gson gson) {
    return fromJson(JsonElement.class, gson);
  }

  /**
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using a {@link com.google.gson.Gson} obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, GsonParseOpts> fromJson(Class<T> type) {
    return fromJson(type, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using a {@link com.google.gson.Gson} obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, GsonParseOpts> fromJson(TypeToken<T> type) {
    return fromJson(type, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using the given {@link com.google.gson.Gson}.
   * If it is {@code null}, a Gson instance will be obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param gson the Gson instance to use to convert the JSON into a Java object
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, GsonParseOpts> fromJson(Class<T> type, @Nullable com.google.gson.Gson gson) {
    return Parse.<T, GsonParseOpts>of(type, new DefaultGsonParseOpts(gson));
  }

  /**
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using the given {@link com.google.gson.Gson}.
   * If it is {@code null}, a Gson instance will be obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param gson the Gson instance to use to convert the JSON into a Java object
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, GsonParseOpts> fromJson(TypeToken<T> type, @Nullable com.google.gson.Gson gson) {
    return Parse.<T, GsonParseOpts>of(type, new DefaultGsonParseOpts(gson));
  }

  /**
   * Renders a data stream as a JSON list, directly streaming the JSON.
   * <p>
   * This method differs from rendering a list of items using {@link #json(Object) json(someList)} in that data is
   * written to the response as chunks, and is streamed.
   * <p>
   * If stream can be very large without using considerable memory as the JSON is streamed incrementally in chunks.
   * This does mean that if on object-to-JSON conversion fails midway through the stream, then the output JSON will be malformed due to being incomplete.
   * If the publisher emits an error, the response will be terminated and no more JSON will be sent.
   * <pre class="java">{@code
   * import ratpack.gson.GsonModule;
   * import ratpack.guice.Guice;
   * import ratpack.test.embed.EmbeddedApp;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.stream.Streams;
   * import org.reactivestreams.Publisher;
   *
   * import java.util.Arrays;
   *
   * import static ratpack.gson.Gson.chunkedJsonList;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *       .registry(Guice.registry(b -> b.module(GsonModule.class)))
   *       .handlers(chain ->
   *         chain.get(ctx -> {
   *           Publisher<Integer> ints = Streams.publish(Arrays.asList(1, 2, 3));
   *           ctx.render(chunkedJsonList(ctx, ints));
   *         })
   *       )
   *     ).test(httpClient -> {
   *       ReceivedResponse response = httpClient.get();
   *       assertEquals("[1,2,3]", response.getBody().getText()); // body was streamed in chunks
   *       assertEquals("application/json", response.getBody().getContentType().getType());
   *     });
   *   }
   * }
   * }</pre>
   * <p>
   * Items of the stream will be converted to JSON by an {@link com.google.gson.Gson} obtained from the given registry.
   * <p>
   * This method uses {@link Streams#streamMap(Publisher, StreamMapper)} to consume the given stream.
   *
   * @param registry the registry to obtain the object mapper from
   * @param stream the stream to render
   * @param <T> the type of item in the stream
   * @return a renderable object
   * @see Streams#streamMap(Publisher, StreamMapper)
   */
  public static <T> ResponseChunks chunkedJsonList(Registry registry, Publisher<T> stream) {
    return chunkedJsonList(registry.get(com.google.gson.Gson.class), stream);
  }

  /**
   * Renders a data stream as a JSON list, directly streaming the JSON.
   * <p>
   * Identical to {@link #chunkedJsonList(Registry, Publisher)}, except uses the given Gson instance instead of obtaining one from the registry.
   *
   * @param gson the Gson instance to use to convert stream items to their JSON representation
   * @param stream the stream to render
   * @param <T> the type of item in the stream
   * @return a renderable object
   * @see #chunkedJsonList(Registry, Publisher)
   */
  public static <T> ResponseChunks chunkedJsonList(com.google.gson.Gson gson, Publisher<T> stream) {
    return ResponseChunks.bufferChunks(HttpHeaderConstants.JSON, Streams.streamMap(stream, (s, out) -> {

      JsonWriter writer = gson.newJsonWriter(new Writer() {
        @Override
        public void write(char[] b, int off, int len) throws IOException {
          out.item(Unpooled.copiedBuffer(b, off, len, Charset.defaultCharset()));
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }
      });

      writer.beginArray();

      return new WriteStream<T>() {
        @Override
        public void item(T item) {
          try {
            gson.toJson(item, item.getClass(), writer);
          } catch (Exception e) {
            s.cancel();
            out.error(e);
          }
        }

        @Override
        public void error(Throwable throwable) {
          out.error(throwable);
        }

        @Override
        public void complete() {
          try {
            writer.endArray();
            writer.close();
            out.complete();
          } catch (IOException e) {
            out.error(e);
          }
        }
      };
    }));
  }

  /**
   * Creates a mapping function that returns the JSON representation as a string of the input object.
   * <p>
   * An {@link com.google.gson.Gson} instance is obtained from the given registry eagerly.
   * The returned function uses the {@link com.google.gson.Gson#toJson(Object)} method to convert the input object to JSON.
   * <pre class="java">{@code
   * import ratpack.gson.GsonModule;
   * import ratpack.guice.Guice;
   * import ratpack.exec.Promise;
   * import ratpack.test.embed.EmbeddedApp;
   * import ratpack.http.client.ReceivedResponse;
   *
   * import java.util.Arrays;
   *
   * import static ratpack.gson.Gson.toJson;
   * import static java.util.Collections.singletonMap;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *       .registry(Guice.registry(b -> b.module(GsonModule.class)))
   *       .handlers(chain -> chain
   *         .get(ctx ->
   *           Promise.value(singletonMap("foo", "bar"))
   *             .map(toJson(ctx))
   *             .then(ctx::render)
   *         )
   *       )
   *     ).test(httpClient -> {
   *       assertEquals("{\"foo\":\"bar\"}", httpClient.getText());
   *     });
   *   }
   * }
   * }</pre>
   * <p>
   * Note that in the above example, it would have been better to just <a href="#rendering">render</a> the result of the blocking call.
   * Doing so would be more convenient and also set the correct {@code "Content-Type"} header.
   * This method can be useful when sending the JSON somewhere else than directly to the response, or when {@link Streams#map(Publisher, Function) mapping streams}.
   *
   * @param registry the registry to obtain the {@link com.google.gson.Gson} from
   * @param <T> the type of object to convert to JSON
   * @return a function that converts objects to their JSON string representation
   */
  public static <T> Function<T, String> toJson(Registry registry) {
    return obj -> registry.get(com.google.gson.Gson.class).toJson(obj);
  }

}
