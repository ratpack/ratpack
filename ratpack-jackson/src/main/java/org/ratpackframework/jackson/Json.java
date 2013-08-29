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

package org.ratpackframework.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ratpackframework.api.Nullable;

/**
 * Represents some object to be rendered as JSON.
 * <p>
 * Designed to be used with Ratpack's rendering framework.
 * </p>
 * <pre class="tested">
 * import org.ratpackframework.handling.Handler;
 * import org.ratpackframework.handling.Context;
 *
 * import static org.ratpackframework.jackson.Json.json;
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
 * <p>
 * A renderer for this type can be provided by the {@link JacksonModule}.
 * </p>
 *
 * @param <T> The type of the object to render as JSON.
 */
public class Json<T> {

  private final T object;
  private final ObjectMapper objectMapper;

  /**
   * Constructs a JSON wrapper around the given object.
   * <p>
   * Generally, use of the {@link #json(Object)} method is preferred over this constructor.
   * </p>
   *
   * @param object The object to render as JSON.
   * @param objectMapper The mapper to use to convert the object to JSON.
   */
  public Json(T object, @Nullable ObjectMapper objectMapper) {
    this.object = object;
    this.objectMapper = objectMapper;
  }

  /**
   * The underlying object to be rendered.
   *
   * @return The underlying object to be rendered.
   */
  public T getObject() {
    return object;
  }

  /**
   * The object mapper to use to render the object as JSON.
   * <p>
   * If null, the "default" mapper should be used by the renderer.
   *
   * @return The object mapper to be used.
   */
  @Nullable
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * Json rendering of the given object, using the default object mapper.
   *
   * @param object The object to render as JSON.
   * @param <T> The type of the object to render as JSON.
   * @return A JSON type wrapper for the given object.
   */
  public static <T> Json<T> json(T object) {
    return new Json<>(object, null);
  }

  /**
   * Json rendering of the given object, using the given object mapper.
   *
   * @param object The object to render as JSON.
   * @param objectMapper The mapper to use to render the object as JSON. If null, the default object mapper will be used by the renderer.
   * @param <T> The type of the object to render as JSON.
   * @return A JSON type wrapper for the given object.
   */
  public static <T> Json<T> json(T object, @Nullable ObjectMapper objectMapper) {
    return new Json<>(object, objectMapper);
  }

}
