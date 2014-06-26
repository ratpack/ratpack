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

package ratpack.test.embed;

import ratpack.server.RatpackServer;

import java.net.URI;

import static ratpack.util.ExceptionUtils.uncheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A support implementation that handles the file system and {@link ratpack.test.ApplicationUnderTest} requirements.
 * <p>
 * Implementations just need to implement {@link #createServer()}.
 */
public abstract class EmbeddedApplicationSupport implements EmbeddedApplication {
  private final static Logger LOGGER = LoggerFactory.getLogger(EmbeddedApplicationSupport.class);

  private RatpackServer ratpackServer;

  /**
   * The server.
   * <p>
   * The first time this method is called, it will call {@link #createServer()}.
   *
   * @return The server
   */
  @Override
  public RatpackServer getServer() {
    if (ratpackServer == null) {
      ratpackServer = createServer();
    }

    return ratpackServer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public URI getAddress() {
    RatpackServer server = getServer();
    try {
      if (!server.isRunning()) {
        server.start();
      }
      return new URI(server.getScheme(), null, server.getBindHost(), server.getBindPort(), "/", null, null);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Subclass implementation hook for creating the server implementation.
   * <p>
   * Only ever called once.
   *
   * @return The server to test
   */
  abstract protected RatpackServer createServer();

  /**
   * Stops the server returned by {@link #getServer()}.
   * <p>
   * Exceptions thrown by calling {@link RatpackServer#stop()} are suppressed and written to {@link System#err System.err}.
   */
  @Override
  public void close() {
    try {
      getServer().stop();
    } catch (Exception e) {
      LOGGER.error("", e);
    }
  }

}
