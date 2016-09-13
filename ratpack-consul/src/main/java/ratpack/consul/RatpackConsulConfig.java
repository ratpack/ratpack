/*
 * Copyright 2016 the original author or authors.
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

package ratpack.consul;

import com.google.common.io.ByteSource;
import com.orbitz.consul.Consul;
import com.orbitz.consul.option.QueryOptions;
import ratpack.func.Action;
import ratpack.util.Exceptions;

/**
 * Integration for retrieving values from a <a href=https://www.consul.io/>Consul</a> Key-Value store as {@link com.google.common.io.ByteSource} to be used
 * with the existing {@link ratpack.server.ServerConfigBuilder} parsing options.
 * @since 1.5
 */
public interface RatpackConsulConfig {

  /**
   * Read the specified key as a {@link ByteSource} using the default Consul agent connection properties.
   *
   * @param key the key to read from Consul's Key-Value store
   * @return a {@link ByteSource} representing the value stored in the key
   * @see #value(String, QueryOptions, Action)
   */
  static ByteSource value(String key) {
    return value(key, QueryOptions.BLANK, Action.noop());
  }

  /**
   * Read the specified key as a {@link ByteSource} using the default Consul agent connection properties and the provided {@link QueryOptions}.
   *
   * @param key the key to read from Consul Key-Value store
   * @param queryOptions the options to use when querying Consul
   * @return a {@link ByteSource} representing the value stored in the key
   * @see #value(String, QueryOptions, Action)
   */
  static ByteSource value(String key, QueryOptions queryOptions) {
    return value(key, queryOptions, Action.noop());
  }

  /**
   * Read the specified key as a {@link ByteSource} using the specified configuration to connection to Consul.
   *
   * @param key the key to read from Consul's KeyValue store
   * @param clientConfig the configuration for the Consul connection
   * @return a {@link ByteSource} representing the value stored in the key
   * @see #value(String, QueryOptions, Action)
   */
  static ByteSource value(String key, Action<? super Consul.Builder> clientConfig) {
    return value(key, QueryOptions.BLANK, clientConfig);
  }

  /**
   * Read the specified key as a {@link ByteSource} using the specified configuration to connection to Consul and the provided {@link QueryOptions}.
   * The returned value can then be passed to the existing parsing options in {@link ratpack.server.ServerConfigBuilder} to provide configuration.
   * <p>
   * <pre class="java-args">{@code
   * import ratpack.consul.RatpackConsulConfig;
   * import ratpack.test.embed.EmbeddedApp;
   * import com.orbitz.consul.option.ImmutableQueryOptions;
   *
   * public class Example {
   *   public static class Config {
   *     public String name;
   *     public String environment;
   *     public String secret;
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(a -> a
   *       .serverConfig(s -> s
   *         .yaml(RatpackConsulConfig.value("default/app"))
   *         .json(RatpackConsulConfig.value("default/environment", ImmutableQueryOptions.builder().token("app-acl-token").build()))
   *         .props(RatpackConsulConfig.value("app/environment", b -> b.withUrl("https://consul.domain.io")))
   *         .require("/config", Config.class)
   *       )
   *       .handlers(c -> c
   *         .get(ctx -> ctx.render(ctx.get(Config.class)))
   *       )
   *     );
   *   }
   * }
   * }</pre>
   * @param key the key to read from Consul Key-Value store
   * @param queryOptions the options to use when querying Consul
   * @param clientConfig he configuration for the Consul connection
   * @return a {@link ByteSource} representing the value stored in the key
   */
  static ByteSource value(String key, QueryOptions queryOptions, Action<? super Consul.Builder> clientConfig) {
    return Exceptions.uncheck(() ->
      ByteSource.wrap(
        clientConfig.with(Consul.builder()).build()
          .keyValueClient()
          .getValue(key, queryOptions)
          .transform(v -> v.getValueAsString().or(""))
          .or("").getBytes()
      )
    );
  }

}
