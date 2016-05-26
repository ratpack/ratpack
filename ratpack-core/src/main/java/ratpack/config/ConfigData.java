/*
 * Copyright 2015 the original author or authors.
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

package ratpack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.reflect.TypeToken;
import ratpack.config.internal.DefaultConfigDataBuilder;
import ratpack.func.Action;
import ratpack.server.internal.ServerEnvironment;
import ratpack.util.Types;

/**
 * Configuration data for the application, potentially built from many sources.
 * <p>
 * A {@link ConfigData} object allows configuration to be “read” as Java objects.
 * The data used to populate the objects is specified when building the configuration data.
 * The static methods of this interface can be used to build a configuration data object.
 *
 * <pre class="java">{@code
 * import com.google.common.collect.ImmutableMap;
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static class MyAppConfig {
 *     private String name;
 *
 *     public String getName() {
 *       return name;
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .serverConfig(c -> c
 *         .props(ImmutableMap.of("server.publicAddress", "http://app.example.com", "app.name", "Ratpack"))
 *         .sysProps()
 *         .require("/app", MyAppConfig.class)
 *       )
 *       .handler(registry ->
 *         ctx -> ctx.render("Hi, my name is " + ctx.get(MyAppConfig.class).getName() + " at " + ctx.getServerConfig().getPublicAddress())
 *       )
 *     ).test(httpClient ->
 *       assertEquals("Hi, my name is Ratpack at http://app.example.com", httpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 *
 * @see ConfigDataBuilder
 */
public interface ConfigData {

  /**
   * Builds a new config data with the default object mapper, from the given definition.
   * <p>
   * The default object mapper is constructed without argument.
   * It then has the following Jackson modules applied implicitly:
   * <ul>
   *     <li>{@link com.fasterxml.jackson.datatype.jdk8.Jdk8Module}</li>
   *     <li>{@link com.fasterxml.jackson.datatype.guava.GuavaModule}</li>
   *     <li>{@link com.fasterxml.jackson.datatype.jsr310.JavaTimeModule}</li>
   * </ul>
   * <p>
   * The {@link com.fasterxml.jackson.databind.DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} feature is disabled.
   * <p>
   * The following features of the JSON factory are enabled:
   * <ul>
   *     <li>{@link com.fasterxml.jackson.core.JsonParser.Feature#ALLOW_UNQUOTED_FIELD_NAMES}</li>
   *     <li>{@link com.fasterxml.jackson.core.JsonParser.Feature#ALLOW_SINGLE_QUOTES}</li>
   * </ul>
   *
   * @param definition the config data definition
   * @return a config data
   * @throws Exception any thrown by building the config data
   */
  static ConfigData of(Action<? super ConfigDataBuilder> definition) throws Exception {
    return definition.with(builder()).build();
  }

  /**
   * Builds a new config data with the specified object mapper, from the given definition.
   * <p>
   * The {@link #of(Action)} method should be favoured, as it applies useful default configuration to the object mapper used.
   *
   * @param objectMapper the object mapper to use for configuration purposes
   * @param definition the config data definition
   * @return a config data
   * @throws Exception any thrown by building the config data
   */
  static ConfigData of(ObjectMapper objectMapper, Action<? super ConfigDataBuilder> definition) throws Exception {
    return definition.with(builder(objectMapper)).build();
  }

  static ConfigDataBuilder builder() {
    return new DefaultConfigDataBuilder(ServerEnvironment.env());
  }

  static ConfigDataBuilder builder(ObjectMapper objectMapper) {
    return new DefaultConfigDataBuilder(ServerEnvironment.env(), objectMapper);
  }

  /**
   * Binds a segment of the configuration data to the specified type.
   *
   * @param pointer a <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a> specifying the point in the configuration data to bind from
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return an instance of the specified type with bound configuration data
   */
  default <O> O get(String pointer, Class<O> type) {
    return getAsConfigObject(pointer, type).getObject();
  }

  ObjectNode getRootNode();

  /**
   * Binds a segment of the configuration data to the specified type.
   *
   * @param pointer a <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a> specifying the point in the configuration data to bind from
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return a config object of the specified type with bound configuration data
   */
  default <O> ConfigObject<O> getAsConfigObject(String pointer, Class<O> type) {
    return getAsConfigObject(pointer, Types.token(type));
  }

  /**
   * Binds a segment of the configuration data to the specified type.
   *
   * @param pointer a <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a> specifying the point in the configuration data to bind from
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return a config object of the specified type with bound configuration data
   * @since 1.4
   */
  <O> ConfigObject<O> getAsConfigObject(String pointer, TypeToken<O> type);

  /**
   * Binds the root of the configuration data to the specified type.
   *
   * @param type the class of the type to bind to
   * @param <O> the type to bind to
   * @return an instance of the specified type with bound configuration data
   */
  default <O> O get(Class<O> type) {
    return get(null, type);
  }

}
