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

package ratpack.core.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import ratpack.func.Nullable;
import ratpack.func.Function;
import ratpack.core.http.ResponseChunks;
import ratpack.core.http.internal.HttpHeaderConstants;
import ratpack.core.jackson.internal.DefaultJsonParseOpts;
import ratpack.core.jackson.internal.DefaultJsonRender;
import ratpack.core.parse.Parse;
import ratpack.exec.registry.Registry;
import ratpack.exec.stream.StreamMapper;
import ratpack.exec.stream.Streams;
import ratpack.exec.stream.WriteStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides key integration points with the Jackson support for dealing with JSON.
 *
 * <h3><a name="rendering">Rendering as JSON</a></h3>
 * <p>
 * The methods that return a {@link JsonRender} are to be used with the {@link ratpack.core.handling.Context#render(Object)} method for serializing objects to the response as JSON.
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.jackson.Jackson;
 * import ratpack.http.client.ReceivedResponse;
 * import com.fasterxml.jackson.databind.ObjectMapper;
 *
 * import static ratpack.jackson.Jackson.json;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   public static class Person {
 *     private final String name;
 *     public Person(String name) {
 *       this.name = name;
 *     }
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .handlers(chain ->
 *         chain.get(ctx -> ctx.render(json(new Person("John"))))
 *       )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertEquals("{\"name\":\"John\"}", response.getBody().getText());
 *       assertEquals("application/json", response.getBody().getContentType().getType());
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h4>Streaming JSON</h4>
 * <p>
 * The {@link #chunkedJsonList(Registry, Publisher)} method can be used for rendering a very large JSON stream/list without buffering the entire list in memory.
 *
 * <h3><a name="parsing">Parsing JSON requests</a></h3>
 * <p>
 * The methods that return a {@link Parse} are to be used with the {@link ratpack.core.handling.Context#parse(ratpack.core.parse.Parse)} method for deserializing request bodies containing JSON.
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.jackson.Jackson;
 * import ratpack.http.client.ReceivedResponse;
 * import com.fasterxml.jackson.databind.JsonNode;
 * import com.fasterxml.jackson.databind.ObjectMapper;
 * import com.fasterxml.jackson.annotation.JsonProperty;
 * import com.google.common.reflect.TypeToken;
 *
 * import java.util.List;
 *
 * import static ratpack.util.Types.listOf;
 * import static ratpack.jackson.Jackson.jsonNode;
 * import static ratpack.jackson.Jackson.fromJson;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   public static class Person {
 *     private final String name;
 *     public Person(@JsonProperty("name") String name) {
 *       this.name = name;
 *     }
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .handlers(chain -> chain
 *         .post("asNode", ctx -> {
 *           ctx.render(ctx.parse(jsonNode()).map(n -> n.get("name").asText()));
 *         })
 *         .post("asPerson", ctx -> {
 *           ctx.render(ctx.parse(fromJson(Person.class)).map(p -> p.getName()));
 *         })
 *         .post("asPersonList", ctx -> {
 *           ctx.render(ctx.parse(fromJson(listOf(Person.class))).map(p -> p.get(0).getName()));
 *         })
 *       )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.requestSpec(s ->
 *         s.body(b -> b.type("application/json").text("{\"name\":\"John\"}"))
 *       ).post("asNode");
 *       assertEquals("John", response.getBody().getText());
 *
 *       response = httpClient.requestSpec(s ->
 *         s.body(b -> b.type("application/json").text("{\"name\":\"John\"}"))
 *       ).post("asPerson");
 *       assertEquals("John", response.getBody().getText());
 *
 *       response = httpClient.requestSpec(s ->
 *         s.body(b -> b.type("application/json").text("[{\"name\":\"John\"}]"))
 *       ).post("asPersonList");
 *       assertEquals("John", response.getBody().getText());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * A {@link ratpack.core.parse.NoOptParserSupport} parser is also rendered for the {@code "application/json"} content type.
 * This allows the use of the {@link ratpack.core.handling.Context#parse(java.lang.Class)} and {@link ratpack.core.handling.Context#parse(com.google.common.reflect.TypeToken)} methods.
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.jackson.Jackson;
 * import ratpack.http.client.ReceivedResponse;
 * import com.fasterxml.jackson.annotation.JsonProperty;
 * import com.fasterxml.jackson.databind.ObjectMapper;
 * import com.google.common.reflect.TypeToken;
 *
 * import java.util.List;
 *
 * import static ratpack.util.Types.listOf;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   public static class Person {
 *     private final String name;
 *     public Person(@JsonProperty("name") String name) {
 *       this.name = name;
 *     }
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .handlers(chain -> chain
 *         .post("asPerson", ctx -> {
 *           ctx.parse(Person.class).then(person -> ctx.render(person.getName()));
 *         })
 *         .post("asPersonList", ctx -> {
 *           ctx.parse(listOf(Person.class)).then(person -> ctx.render(person.get(0).getName()));
 *         })
 *       )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.requestSpec(s ->
 *         s.body(b -> b.type("application/json").text("{\"name\":\"John\"}"))
 *       ).post("asPerson");
 *       assertEquals("John", response.getBody().getText());
 *
 *       response = httpClient.requestSpec(s ->
 *         s.body(b -> b.type("application/json").text("[{\"name\":\"John\"}]"))
 *       ).post("asPersonList");
 *       assertEquals("John", response.getBody().getText());
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3 id="configuring-jackson">Configuring Jackson</h3>
 * <p>
 * The Jackson API is based around the {@link ObjectMapper}.
 * Ratpack adds a default instance to the base registry automatically.
 * To configure Jackson behaviour, override this instance.
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.http.client.ReceivedResponse;
 * import com.fasterxml.jackson.databind.ObjectMapper;
 * import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
 *
 * import java.util.Optional;
 *
 * import static ratpack.jackson.Jackson.json;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   public static class Person {
 *     private final String name;
 *     public Person(String name) {
 *       this.name = name;
 *     }
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registryOf(r -> r
 *         .add(new ObjectMapper().registerModule(new Jdk8Module()))
 *       )
 *       .handlers(chain ->
 *         chain.get(ctx -> {
 *           Optional<Person> personOptional = Optional.of(new Person("John"));
 *           ctx.render(json(personOptional));
 *         })
 *       )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertEquals("{\"name\":\"John\"}", response.getBody().getText());
 *       assertEquals("application/json", response.getBody().getContentType().getType());
 *     });
 *   }
 * }
 * }</pre>
 */
public abstract class Jackson {

  private Jackson() {
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#render renderable object} to render the given object as JSON.
   * <p>
   * The given object will be converted to JSON using an {@link ObjectWriter} obtained from the context registry.
   * <p>
   * See the <a href="#rendering">rendering</a> section for usage examples.
   *
   * @param object the object to render as JSON
   * @return a renderable wrapper for the given object
   */
  public static JsonRender json(Object object) {
    return new DefaultJsonRender(object, null, null);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#render renderable object} to render the given object as JSON.
   * <p>
   * The given object will be converted to JSON using the given {@link ObjectWriter}.
   * If it is {@code null}, an {@code ObjectWriter} will be obtained from the context registry.
   * <p>
   * See the <a href="#rendering">rendering</a> section for usage examples.
   *
   * @param object the object to render as JSON
   * @param objectWriter the object writer to use to serialize the object to JSON
   * @return a renderable wrapper for the given object
   */
  public static JsonRender json(Object object, @Nullable ObjectWriter objectWriter) {
    return new DefaultJsonRender(object, objectWriter);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#render renderable object} to render the given object as JSON.
   * <p>
   * The given object will be converted to JSON using an {@link ObjectWriter} obtained from the context registry
   * with the specified view {@code Class} used to determine which fields are included.
   * If it is null the default view rendering of the {@link ObjectWriter} will be used.
   * <p>
   * See the <a href="#rendering">rendering</a> section for usage examples.
   *
   * @param object the object to render as JSON
   * @param viewClass the view to use when rendering
   * @return a renderable wrapper for the given object
   */
  public static JsonRender json(Object object, @Nullable Class<?> viewClass) {
    return new DefaultJsonRender(object, viewClass);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#render renderable object} to render the given object as JSON.
   * <p>
   * The given object will be converted to JSON using the given {@link ObjectWriter}
   * with the specified view {@code Class} used to determine which fields are included.
   * If the {@link ObjectWriter} is {@code null}, an {@code ObjectWriter} will be obtained from the context registry.
   * If the view {@code Class} is null the default view rendering of the {@link ObjectWriter} will be used.
   * <p>
   * See the <a href="#rendering">rendering</a> section for usage examples.
   *
   * @param object the object to render as JSON
   * @param objectWriter the object writer to use to serialize the object to JSON
   * @param viewClass the view to use when rendering
   * @return a renderable wrapper for the given object
   */
  public static JsonRender json(Object object, @Nullable ObjectWriter objectWriter, @Nullable Class<?> viewClass) {
    return new DefaultJsonRender(object, objectWriter, viewClass);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#parse parseable object} to parse a request body into a {@link JsonNode}.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using an {@link ObjectMapper} obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @return a parse object
   */
  public static Parse<JsonNode, JsonParseOpts> jsonNode() {
    return jsonNode(null);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#parse parseable object} to parse a request body into a {@link JsonNode}.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using the given {@link ObjectMapper}.
   * If it is {@code null}, a mapper will be obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param objectMapper the object mapper to use to parse the JSON
   * @return a parse object
   */
  public static Parse<JsonNode, JsonParseOpts> jsonNode(@Nullable ObjectMapper objectMapper) {
    return fromJson(JsonNode.class, objectMapper);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using an {@link ObjectMapper} obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, JsonParseOpts> fromJson(Class<T> type) {
    return fromJson(type, null);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using an {@link ObjectMapper} obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, JsonParseOpts> fromJson(TypeToken<T> type) {
    return fromJson(type, null);
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using the given {@link ObjectMapper}.
   * If it is {@code null}, a mapper will be obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param objectMapper the object mapper to use to convert the JSON into a Java object
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, JsonParseOpts> fromJson(Class<T> type, @Nullable ObjectMapper objectMapper) {
    return Parse.<T, JsonParseOpts>of(type, new DefaultJsonParseOpts(objectMapper));
  }

  /**
   * Creates a {@link ratpack.core.handling.Context#parse parseable object} to parse a request body into the given type.
   * <p>
   * The corresponding parser for this type requires the request content type to be {@code "application/json"}.
   * <p>
   * The request body will be parsed using the given {@link ObjectMapper}.
   * If it is {@code null}, a mapper will be obtained from the context registry.
   * <p>
   * See the <a href="#parsing">parsing</a> section for usage examples.
   *
   * @param type the type of object to deserialize the JSON into
   * @param objectMapper the object mapper to use to convert the JSON into a Java object
   * @param <T> the type of object to deserialize the JSON into
   * @return a parse object
   */
  public static <T> Parse<T, JsonParseOpts> fromJson(TypeToken<T> type, @Nullable ObjectMapper objectMapper) {
    return Parse.<T, JsonParseOpts>of(type, new DefaultJsonParseOpts(objectMapper));
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
   * import ratpack.test.embed.EmbeddedApp;
   * import ratpack.jackson.Jackson;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.stream.Streams;
   * import com.fasterxml.jackson.databind.ObjectMapper;
   * import org.reactivestreams.Publisher;
   *
   * import java.util.Arrays;
   *
   * import static ratpack.jackson.Jackson.chunkedJsonList;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
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
   * Items of the stream will be converted to JSON by an {@link ObjectMapper} obtained from the given registry.
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
    return chunkedJsonList(getObjectWriter(registry), stream);
  }

  public static ObjectWriter getObjectWriter(Registry registry) {
    return registry.maybeGet(ObjectWriter.class)
      .orElseGet(() -> registry.get(ObjectMapper.class).writer());
  }

  /**
   * Renders a data stream as a JSON list, directly streaming the JSON.
   * <p>
   * Identical to {@link #chunkedJsonList(Registry, Publisher)}, except uses the given object writer instead of obtaining one from the registry.
   *
   * @param objectWriter the object write to use to convert stream items to their JSON representation
   * @param stream the stream to render
   * @param <T> the type of item in the stream
   * @return a renderable object
   * @see #chunkedJsonList(Registry, Publisher)
   */
  public static <T> ResponseChunks chunkedJsonList(ObjectWriter objectWriter, Publisher<T> stream) {
    return ResponseChunks.bufferChunks(HttpHeaderConstants.JSON, Streams.streamMap(stream, (s, out) -> {
      JsonGenerator generator = objectWriter.getFactory().createGenerator(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
          throw new UnsupportedOperationException();
        }

        @Override
        public void write(@SuppressWarnings("NullableProblems") byte[] b, int off, int len) throws IOException {
          out.item(Unpooled.copiedBuffer(b, off, len));
        }
      });

      generator.writeStartArray();

      return new WriteStream<T>() {
        @Override
        public void item(T item) {
          try {
            generator.writeObject(item);
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
            generator.writeEndArray();
            generator.close();
            out.complete();
          } catch (IOException e) {
            out.error(e);
          }
        }
      };
    }));
  }

  /**
   * Deprecated.
   *
   * @deprecated since 1.10 with no replacement
   */
  @Deprecated
  public static <T> Function<T, String> toJson(Registry registry) {
    return getObjectWriter(registry)::writeValueAsString;
  }

}
