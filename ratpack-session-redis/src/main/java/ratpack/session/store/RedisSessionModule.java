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

package ratpack.session.store;

import com.google.inject.Scopes;
import ratpack.guice.ConfigurableModule;
import ratpack.session.SessionStore;
import ratpack.session.store.internal.RedisSessionStore;

/**
 * An extension module that provides a redis backed session store.
 * <p>
 * This module depends on {@link ratpack.session.SessionModule} and <b>MUST</b> be added to the module list <b>AFTER</b> {@link ratpack.session.SessionModule}.
 *
 * <h3>Example usage</h3>
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.session.Session;
 * import ratpack.session.SessionKey;
 * import ratpack.session.SessionModule;
 * import ratpack.session.store.RedisSessionModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.*;
 *
 * public class ClientSideSessionModuleExample {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b -> b
 *         .module(SessionModule.class, config -> {
 *           //config.path("/").domain("www.example.com");
 *         })
 *         .module(RedisSessionModule.class, config -> {
 *           config.setHost("127.0.0.1");
 *           config.setSessionKeyPrefix("prefix");
 *         })
 *       ))
 *       .handlers(chain -> chain
 *         .get(ctx -> {
 *           ctx.get(Session.class).getData()
 *             .map(d -> d.get(SessionKey.ofType("value", "value")).orElse("not set"))
 *             .then(ctx::render);
 *         })
 *         .get("set/:value", ctx -> {
 *           ctx.get(Session.class).getData().then(d -> {
 *             d.set("value", ctx.getPathTokens().get("value"));
 *             ctx.render(d.get(SessionKey.ofType("value", "value")).orElse("not set"));
 *           });
 *         })
 *       )
 *     )
 *     .test(client -> {
 *       ReceivedResponse response = client.get();
 *       assertEquals("not set", response.getBody().getText());
 *
 *       response = client.get("set/foo");
 *       assertEquals("foo", response.getBody().getText());
 *
 *       response = client.get();
 *       assertEquals("foo", response.getBody().getText());
 *     });
 *   }
 * }
 * }</pre>
 *
 */
public class RedisSessionModule extends ConfigurableModule<RedisSessionModule.Config> {

  @Override
  protected void configure() {
    bind(SessionStore.class).to(RedisSessionStore.class).in(Scopes.SINGLETON);
  }

  /**
   *  Configuartion for Redis Session Storage.
   */
  public static class Config {
    String password;
    String host;
    String port;
    String sessionKeyPrefix = "";


    public Config() {
      host = "127.0.0.1";
    }

    /**
     *
     * @param password Redis Password
     * @param host Redis host address
     * @param port Redis port to use
     */
    public Config(String password, String host, String port) {
      this.password = password;
      this.host = host;
      this.port = port;
    }

    /**
     *
     * @return The password configured to use with Redis
     */
    public String getPassword() {
      return password;
    }

    /**
     *
     * @param password The password to use when connecting to Redis
     */
    public void setPassword(String password) {
      this.password = password;
    }

    /**
     *
     * @return String of the host address for Redis
     */
    public String getHost() {
      return host;
    }

    /**
     *
     * @param host The address for Redis
     */
    public void setHost(String host) {
      this.host = host;
    }

    /**
     *
     * @return The port for Redis
     */
    public String getPort() {
      return port;
    }

    /**
     *
     * @param port Which port to use for Redis
     */
    public void setPort(String port) {
      this.port = port;
    }

    /**
     *
     * @return The prefix used as part of the key in Redis
     */
    public String getSessionKeyPrefix() {
      return sessionKeyPrefix;
    }

    /**
     * The prefix is used along with the session id to generate the key used to store data in Redis.
     * <p>
     * This can be used to make sure your application never accidentally overrides the session data.
     *
     * @param sessionKeyPrefix What prefix to use for Keys in Redis
     */
    public void setSessionKeyPrefix(String sessionKeyPrefix) {
      this.sessionKeyPrefix = sessionKeyPrefix;
    }
  }
}
