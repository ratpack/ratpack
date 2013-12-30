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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * A support implementation that handles the file system and {@link ratpack.test.ApplicationUnderTest} requirements.
 * <p>
 * Implementations just need to implement {@link #createServer()}.
 */
public abstract class EmbeddedApplicationSupport implements EmbeddedApplication {

  private final Path baseDir;
  private RatpackServer ratpackServer;

  /**
   * Constructor.
   *
   * @param baseDir The base dir
   */
  public EmbeddedApplicationSupport(Path baseDir) {
    if (!Files.exists(baseDir)) {
      throw new IllegalArgumentException("baseDir path (" + baseDir + ") does not exist");
    }
    if (!Files.isDirectory(baseDir)) {
      throw new IllegalArgumentException("baseDir path (" + baseDir + ") is not a directory");
    }
    this.baseDir = baseDir;
  }

  /**
   * The {@code baseDir} argument given at construction time.
   *
   * @return The {@code baseDir} argument given at construction time
   */
  @Override
  public Path getBaseDir() {
    return baseDir;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File file(String path) {
    return path(path).toFile();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path path(String path) {
    Path file = baseDir.resolve(path);
    Path parent = file.getParent();
    try {
      Files.createDirectories(parent);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return file;
  }

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
      e.printStackTrace(System.err);
    }
  }

}
