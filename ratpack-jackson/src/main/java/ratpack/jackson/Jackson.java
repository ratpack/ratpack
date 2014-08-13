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
import ratpack.jackson.internal.DefaultJsonParseOpts;
import ratpack.jackson.internal.DefaultJsonRender;
import ratpack.parse.Parse;


/**
 * Provides key integration points with the Jackson support for dealing with JSON.
 * <p>
 * The {@link ratpack.jackson.JacksonModule} Guice module provides the infrastructure necessary to use these functions.
 * </p>
 * <h3>Rendering as JSON</h3>
 * <p>
 * The methods that return a {@link JsonRender} are to be used with the {@link ratpack.handling.Context#render(Object)} method for serializing
 * objects to the response as JSON.
 * </p>
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 *
 * import static ratpack.jackson.Jackson.json;
 *
 * public class MyHandler implements Handler {
 *   public void handle(Context context) {
 *     Person person = new Person("John");
 *     context.render(json(person));
 *   }
 * }
 *
 * public class Person {
 *   private final String name;
 *   public Person(String name) {
 *     this.name = name;
 *   }
 * }
 * </pre>
 * <h3>Parsing JSON requests</h3>
 * <p>
 * The methods that return a {@link JsonParseOpts} are to be used with the {@link ratpack.handling.Context#parse(ratpack.parse.Parse)} method for deserializing
 * request bodies containing JSON.
 * </p>
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import com.fasterxml.jackson.databind.JsonNode;
 *
 * import static ratpack.jackson.Jackson.jsonNode;
 *
 * public class MyHandler implements Handler {
 *   public void handle(Context context) {
 *     JsonNode node = context.parse(jsonNode())
 *     context.render(node.get("someKey"));
 *   }
 * }
 * </pre>
 */
public abstract class Jackson {

  /**
   * Jackson rendering of the given object, using the default object writer.
   *
   * @param object The object to render as JSON.
   * @param <T> The type of the object to render as JSON.
   * @return A JSON type wrapper for the given object.
   */
  public static <T> JsonRender<T> json(T object) {
    return new DefaultJsonRender<>(object, null);
  }

  /**
   * Jackson rendering of the given object, using the given object writer.
   *
   * @param object The object to render as JSON.
   * @param objectWriter The writer to use to render the object as JSON. If null, the default object writer will be used by the renderer.
   * @param <T> The type of the object to render as JSON.
   * @return A JSON type wrapper for the given object.
   */
  public static <T> JsonRender<T> json(T object, @Nullable ObjectWriter objectWriter) {
    return new DefaultJsonRender<>(object, objectWriter);
  }

  public static Parse<JsonNode, JsonParseOpts> jsonNode() {
    return jsonNode(null);
  }

  public static Parse<JsonNode, JsonParseOpts> jsonNode(@Nullable ObjectMapper objectMapper) {
    return fromJson(JsonNode.class, objectMapper);
  }

  public static <T> Parse<T, JsonParseOpts> fromJson(Class<T> type) {
    return fromJson(type, null);
  }

  public static <T> Parse<T, JsonParseOpts> fromJson(TypeToken<T> type) {
    return fromJson(type, null);
  }

  public static <T> Parse<T, JsonParseOpts> fromJson(Class<T> type, @Nullable ObjectMapper objectMapper) {
    return Parse.<T, JsonParseOpts>of(type, new DefaultJsonParseOpts(objectMapper));
  }

  public static <T> Parse<T, JsonParseOpts> fromJson(TypeToken<T> type, @Nullable ObjectMapper objectMapper) {
    return Parse.<T, JsonParseOpts>of(type, new DefaultJsonParseOpts(objectMapper));
  }

}
