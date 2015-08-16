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

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import ratpack.func.Action;
import ratpack.func.Function;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * Configures how configuration data will be loaded and bound to objects.
 * <p>
 * Multiple data sources can be specified.
 * All specified data sources will be merged together to form the final configuration data.
 * If a given value exists in multiple data sources, the value from the last specified source will be used.
 * <p>
 * By default, if loading a data source fails, the exception will be thrown.
 * If desired, this behavior can be adjusted using {@link #onError(ratpack.func.Action)}.
 * For example:
 *
 * <pre class="java">{@code
 * import java.nio.file.Files;
 * import java.nio.file.Path;
 * import ratpack.func.Action;
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.test.http.TestHttpClient;
 *
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     Path jsonFile = Files.createTempFile("optionalConfig", ".json");
 *     Files.delete(jsonFile);
 *     Path yamlFile = Files.createTempFile("mandatoryConfig", ".yaml");
 *     try {
 *       Files.write(yamlFile, "server:\n  threads: 7\n  port: 0".getBytes());
 *       RatpackServer server = RatpackServer.of(spec -> {
 *         ServerConfig serverConfig = ServerConfig
 *           .embedded()
 *           .onError(Action.noop()).json(jsonFile)
 *           .onError(Action.throwException()).yaml(yamlFile)
 *           .build();
 *         spec
 *           .serverConfig(serverConfig)
 *           .handler(registry ->
 *             ctx -> ctx.render("threads:" + ctx.get(ServerConfig.class).getThreads())
 *           );
 *       });
 *       server.start();
 *
 *       TestHttpClient httpClient = TestHttpClient.testHttpClient(server);
 *       assertEquals("threads:7", httpClient.getText());
 *
 *       server.stop();
 *     } finally {
 *       Files.delete(yamlFile);
 *     }
 *   }
 * }
 * }</pre>
 *
 * @see ConfigData#builder()
 */
public interface ConfigDataBuilder {

  String DEFAULT_ENV_PREFIX = "RATPACK_";
  String DEFAULT_PROP_PREFIX = "ratpack.";

  /**
   * Creates the config data, based on the state of this builder.
   *
   * @return a new config data
   */
  ConfigData build();

  /**
   * Configures the object mapper used for binding configuration data to arbitrary objects.
   *
   * @param action an action to perform upon the object mapper
   * @return {@code this}
   */
  ConfigDataBuilder configureObjectMapper(Action<ObjectMapper> action);

  /**
   * Adds a configuration source.
   *
   * @param configSource the configuration source to add
   * @return {@code this}
   */
  ConfigDataBuilder add(ConfigSource configSource);

  /**
   * Adds a configuration source for environment variables starting with the prefix {@value #DEFAULT_ENV_PREFIX}.
   * <p>
   * The prefix will be removed before loading the data.
   * The environment variable name is split into per-object segments using double underscore as an object boundary.
   * Segments are transformed into camel-case field names using a single underscore as a word boundary.
   *
   * @return this
   */
  ConfigDataBuilder env();

  /**
   * Adds a configuration source for environment variables starting with the specified prefix.
   * The prefix will be removed before loading the data.
   * The environment variable name is split into per-object segments using double underscore as an object boundary.
   * Segments are transformed into camel-case field names using a single underscore as a word boundary.
   *
   * @param prefix the prefix which should be used to identify relevant environment variables
   * @return this
   */
  ConfigDataBuilder env(String prefix);

  /**
   * Adds a configuration source for environment variables starting with the specified prefix.
   * The prefix will be removed before loading the data.
   * The environment variable name is split into per-object segments using double underscore as an object boundary.
   * Segments are transformed into field names using the specified transformation function rather than the default function.
   *
   * @param prefix the prefix which should be used to identify relevant environment variables
   * @param mapFunc the function to transform segments into field names
   * @return this
   */
  ConfigDataBuilder env(String prefix, Function<String, String> mapFunc);

  /**
   * Adds a configuration source for environment variables using custom parsing logic.
   *
   * @param environmentParser the parser to use to interpret environment variables
   * @return {@code this}
   */
  ConfigDataBuilder env(EnvironmentParser environmentParser);

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param byteSource the source of the JSON data
   * @return {@code this}
   */
  ConfigDataBuilder json(ByteSource byteSource);

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param path the source of the JSON data
   * @return {@code this}
   */
  ConfigDataBuilder json(Path path);

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param path the path to the source of the JSON data
   * @return {@code this}
   */
  default ConfigDataBuilder json(String path) {
    return json(Paths.get(path));
  }

  /**
   * Adds a configuration source for a JSON file.
   *
   * @param url the source of the JSON data
   * @return {@code this}
   */
  ConfigDataBuilder json(URL url);

  /**
   * Adds a configuration source for a properties file.
   *
   * @param byteSource the source of the properties data
   * @return {@code this}
   */
  ConfigDataBuilder props(ByteSource byteSource);

  /**
   * Adds a configuration source for a properties file.
   *
   * @param path the source of the properties data
   * @return {@code this}
   */
  ConfigDataBuilder props(Path path);

  /**
   * Adds a configuration source for a properties object.
   *
   * @param properties the properties object
   * @return {@code this}
   */
  ConfigDataBuilder props(Properties properties);

  /**
   * Adds a configuration source for a Map (flat key-value pairs).
   *
   * This signature is particularly useful for providing default values as shown below:
   *
   * <pre class="java">{@code
   * import com.google.common.collect.ImmutableMap;
   * import ratpack.config.ConfigData;
   * import ratpack.server.ServerConfig;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     ServerConfig serverConfig = ServerConfig
   *       .builder()
   *       .props(ImmutableMap.of("server.port", "5060"))
   *       .sysProps()
   *       .build();
   *     assertEquals(5060, serverConfig.getPort());
   *   }
   * }
   * }</pre>
   *
   * @param map the map
   * @return {@code this}
   */
  ConfigDataBuilder props(Map<String, String> map);

  /**
   * Adds a configuration source for a properties file.
   *
   * @param path the path to the source of the properties data
   * @return {@code this}
   */
  default ConfigDataBuilder props(String path) {
    return props(Paths.get(path));
  }

  /**
   * Adds a configuration source for a properties file.
   *
   * @param url the source of the properties data
   * @return {@code this}
   */
  ConfigDataBuilder props(URL url);

  /**
   * Adds a configuration source for system properties starting with the prefix {@value #DEFAULT_PROP_PREFIX}.
   *
   * @return {@code this}
   */
  ConfigDataBuilder sysProps();

  /**
   * Adds a configuration source for system properties starting with the specified prefix.
   *
   * @param prefix the prefix which should be used to identify relevant system properties;
   * the prefix will be removed before loading the data
   * @return {@code this}
   */
  ConfigDataBuilder sysProps(String prefix);

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param byteSource the source of the YAML data
   * @return {@code this}
   */
  ConfigDataBuilder yaml(ByteSource byteSource);

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param path the source of the YAML data
   * @return {@code this}
   */
  ConfigDataBuilder yaml(Path path);

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param path the path to the source of the YAML data
   * @return {@code this}
   */
  default ConfigDataBuilder yaml(String path) {
    return yaml(Paths.get(path));
  }

  /**
   * Adds a configuration source for a YAML file.
   *
   * @param url the source of the YAML data
   * @return {@code this}
   */
  ConfigDataBuilder yaml(URL url);

  /**
   * Sets the error all that will be used for added configuration sources.
   * The error all only applies to configuration sources added after this method is called; it is not applied retroactively.
   *
   * @param errorHandler the error all
   * @return {@code this}
   * @see ratpack.func.Action#noop()
   * @see ratpack.func.Action#throwException()
   */
  ConfigDataBuilder onError(Action<? super Throwable> errorHandler);

  /**
   * Returns the object mapper used for configuration binding.
   *
   * @return the object mapper
   */
  ObjectMapper getObjectMapper();

  /**
   * Adds {@link Module Jackson modules} to the object mapper.
   *
   * @param modules the modules to add
   * @return this
   * @see ObjectMapper#registerModule(Module)
   */
  default ConfigDataBuilder jacksonModules(Module... modules) {
    getObjectMapper().registerModules(modules);
    return this;
  }

  /**
   * Returns the config sources used for configuration binding.
   * <p>
   * Add sources via {@link #add(ConfigSource)}.
   *
   * @return the config sources
   */
  ImmutableList<ConfigSource> getConfigSources();

  ConfigDataBuilder baseDir(Path baseDir);

  Path getBaseDir();
}
