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

package ratpack.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.reflect.TypeToken;
import ratpack.api.Nullable;
import ratpack.jackson.internal.*;
import ratpack.parse.NullParseOpts;
import ratpack.parse.Parse;
import ratpack.parse.Parser;
import ratpack.registry.RegistrySpec;
import ratpack.render.Renderer;

/**
 * Provides key integration points with the Jackson support for dealing with JSON.
 * <p>
 * The {@link ratpack.jackson.JacksonModule} Guice module provides the infrastructure necessary to use these functions.
 *
 * <h3><a name="rendering">Rendering as JSON</a></h3>
 * <p>
 * The methods that return a {@link JsonRender} are to be used with the {@link ratpack.handling.Context#render(Object)} method for serializing objects to the response as JSON.
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.jackson.JacksonModule;
 * import ratpack.http.client.ReceivedResponse;
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
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(launchConfig ->
 *       Guice.builder(launchConfig)
 *         .bindings(b ->
 *           b.add(JacksonModule.class, c -> c.prettyPrint(false))
 *         )
 *         .build(chain ->
 *           chain.get(ctx -> ctx.render(json(new Person("John"))))
 *         )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertEquals("{\"name\":\"John\"}", response.getBody().getText());
 *       assertEquals("application/json", response.getBody().getContentType().getType());
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3><a name="parsing">Parsing JSON requests</a></h3>
 * <p>
 * The methods that return a {@link Parse} are to be used with the {@link ratpack.handling.Context#parse(ratpack.parse.Parse)} method for deserializing request bodies containing JSON.
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.jackson.JacksonModule;
 * import ratpack.http.client.ReceivedResponse;
 * import com.fasterxml.jackson.databind.JsonNode;
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
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(launchConfig ->
 *       Guice.builder(launchConfig)
 *         .bindings(b ->
 *           b.add(JacksonModule.class, c -> c.prettyPrint(false))
 *         )
 *         .build(chain -> chain
 *           .post("asNode", ctx -> {
 *             JsonNode node = ctx.parse(jsonNode());
 *             ctx.render(node.get("name").asText());
 *           })
 *           .post("asPerson", ctx -> {
 *             Person person = ctx.parse(fromJson(Person.class));
 *             ctx.render(person.getName());
 *           })
 *           .post("asPersonList", ctx -> {
 *             List<Person> person = ctx.parse(fromJson(listOf(Person.class)));
 *             ctx.render(person.get(0).getName());
 *           })
 *         )
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
 * A {@link ratpack.parse.NoOptParserSupport} parser is also rendered for the {@code "application/json"} content type.
 * This allows the use of the {@link ratpack.handling.Context#parse(java.lang.Class)} and {@link ratpack.handling.Context#parse(com.google.common.reflect.TypeToken)} methods.
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.jackson.JacksonModule;
 * import ratpack.http.client.ReceivedResponse;
 * import com.fasterxml.jackson.annotation.JsonProperty;
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
 *   public static void main(String... args) {
 *     EmbeddedApp.fromHandlerFactory(launchConfig ->
 *       Guice.builder(launchConfig)
 *         .bindings(b ->
 *           b.add(JacksonModule.class, c -> c.prettyPrint(false))
 *         )
 *         .build(chain -> chain
 *           .post("asPerson", ctx -> {
 *             Person person = ctx.parse(Person.class);
 *             ctx.render(person.getName());
 *           })
 *           .post("asPersonList", ctx -> {
 *             List<Person> person = ctx.parse(listOf(Person.class));
 *             ctx.render(person.get(0).getName());
 *           })
 *         )
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
 */
public abstract class Jackson {

  private Jackson() {
  }

  /**
   * Creates a {@link ratpack.handling.Context#render renderable object} to render the given object as JSON.
   * <p>
   * The given object will be converted to JSON using an {@link ObjectWriter} obtained from the context registry.
   * <p>
   * See the <a href="#rendering">rendering</a> section for usage examples.
   *
   * @param object the object to render as JSON
   * @return a renderable wrapper for the given object
   */
  public static JsonRender json(Object object) {
    return new DefaultJsonRender(object, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render renderable object} to render the given object as JSON.
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
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into a {@link JsonNode}.
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
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into a {@link JsonNode}.
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
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
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
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
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
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
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
   * Creates a {@link ratpack.handling.Context#parse parseable object} to parse a request body into the given type.
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
   * Factories for Ratpack specific integration types.
   * <p>
   * Use of these methods are not required at all if using the Guice integration and {@link JacksonModule}.
   */
  public static abstract class Init {

    /**
     * The renderer.
     *
     * @param objectWriter the object writer to use to render object
     * @return a JSON renderer
     */
    public static Renderer<JsonRender> renderer(ObjectWriter objectWriter) {
      return new JsonRenderer(objectWriter);
    }

    /**
     * The no-opts parser.
     *
     * @return a JSON parser
     */
    public static Parser<NullParseOpts> noOptParser() {
      return new JsonNoOptParser();
    }

    /**
     * The parser.
     *
     * @param objectMapper the object mapper to use for parsing
     * @return a JSON parser
     */
    public static Parser<JsonParseOpts> parser(ObjectMapper objectMapper) {
      return new JsonParser(objectMapper);
    }

    /**
     * Registers the renderer and parsers with the given registry.
     * <p>
     * If using Jackson support without Guice and the {@link JacksonModule}, this method should be used to register the renderer and parsers with the context registry.
     * Use of this method is not necessary if using {@link JacksonModule} as it makes the renderer and parsers available.
     *
     * @param registrySpec the registry to register with
     * @param objectMapper the object mapper for parsing requests
     * @param objectWriter the object writer for rendering to responses
     * @return the given registry spec
     */
    public static RegistrySpec register(RegistrySpec registrySpec, ObjectMapper objectMapper, ObjectWriter objectWriter) {
      return registrySpec
        .add(new TypeToken<Renderer<JsonRender>>() {}, renderer(objectWriter))
        .add(new TypeToken<Parser<NullParseOpts>>() {}, noOptParser())
        .add(new TypeToken<Parser<JsonParseOpts>>() {}, parser(objectMapper));
    }

  }

}
