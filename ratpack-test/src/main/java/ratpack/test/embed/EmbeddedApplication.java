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
import ratpack.test.ApplicationUnderTest;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;

/**
 * An application created and used at runtime, useful for functionally testing subsets of functionality.
 * <p>
 * This mechanism can be used for functionally testing isolated sections of an application,
 * or for testing general libraries that provide reusable functionality (e.g. Ratpack Guice modules).
 * <p>
 * Different implementations expose different API that can be used to define the actual application under test.
 * <p>
 * As embedded applications also implement {@link ratpack.test.ApplicationUnderTest}, they are suitable for use with clients accessing the app via HTTP.
 * Implementations must ensure that the application is up and receiving request when returning from {@link #getAddress()}.
 * Be sure to {@link #close()} the application after use to free resources.
 *
 * @see ratpack.test.embed.LaunchConfigEmbeddedApplication
 */
public interface EmbeddedApplication extends ApplicationUnderTest, Closeable {

  /**
   * Working space that can be used to support the application.
   * <p>
   * This space must be writable, and generally should be based on some unique temporary space.
   * If using JUnit, the <a href="http://junit.org/javadoc/4.9/org/junit/rules/TemporaryFolder.html">TemporaryFolder</a> rule can be useful.
   * <p>
   * This directory often becomes the base dir of the application at runtime, but implementations are free to do otherwise.
   *
   * @return Working space for defining the application
   */
  Path getBaseDir();

  /**
   * Creates a file object for the given path, relative to the {@link #getBaseDir()}.
   * <p>
   * Implementations must ensure that the parent file of the returned file does exist (creating it if necessary).
   * <p>
   * Is shorthand for {@code this.path(path).toFile()}, so may throw {@link java.lang.UnsupportedOperationException} as per the {@link Path#toFile()} method.
   *
   * @param path The path to the file, relative to the {@link #getBaseDir()}
   * @return The file at the given path, relative to the {@link #getBaseDir()}, whose parent file is guaranteed to exist and be a directory
   */
  File file(String path);

  /**
   * Creates a path object for the given path, relative to the {@link #getBaseDir()}.
   * <p>
   * Implementations must ensure that the parent path of the returned path does exist (creating it if necessary).
   *
   * @param path The path to the path, relative to the {@link #getBaseDir()}
   * @return The path at the given path, relative to the {@link #getBaseDir()}, whose parent path is guaranteed to exist and be a directory
   */
  Path path(String path);

  /**
   * The server for the application.
   * <p>
   * Calling this method does not implicitly start the server.
   *
   * @return The server for the application
   */
  RatpackServer getServer();

}
