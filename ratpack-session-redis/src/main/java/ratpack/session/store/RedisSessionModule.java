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
 */
public class RedisSessionModule extends ConfigurableModule<RedisSessionModule.Config> {

  @Override
  protected void configure() {
    bind(SessionStore.class).to(RedisSessionStore.class).in(Scopes.SINGLETON);
  }

  /**
   * Configuration for Redis Session Storage.
   */
  public static class Config {
    private String password;
    private String host;
    private Integer port;

    public Config() {
      host = "127.0.0.1";
    }

    /**
     * Convenience constructor most of the time not used if you are using Ratpack Config.
     *
     * @param password Redis Password
     * @param host Redis host address
     * @param port Redis port to use
     */
    public Config(String password, String host, Integer port) {
      this.password = password;
      this.host = host;
      this.port = port;
    }

    /**
     * Get the password for Redis.
     *
     * @return The password configured to use with Redis
     */
    public String getPassword() {
      return password;
    }

    /**
     * Set the password for Redis.
     *
     * @param password The password to use when connecting to Redis
     */
    public void setPassword(String password) {
      this.password = password;
    }

    /**
     * Get the address for Redis.
     *
     * @return String of the host address for Redis
     */
    public String getHost() {
      return host;
    }

    /**
     * Set the address for Redis.
     *
     * @param host The address for Redis
     */
    public void setHost(String host) {
      this.host = host;
    }

    /**
     * The Redis port.
     *
     * @return The port for Redis
     */
    public Integer getPort() {
      return port;
    }

    /**
     * Set the redis port.
     *
     * @param port Which port to use for Redis
     */
    public void setPort(Integer port) {
      this.port = port;
    }
  }
}
