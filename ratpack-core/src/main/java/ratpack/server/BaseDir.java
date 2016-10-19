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

package ratpack.server;

import ratpack.server.internal.BaseDirFinder;

import java.nio.file.Path;

public abstract class BaseDir {

  /**
   * The default name for the base dir sentinel properties file.
   * <p>
   * Value: {@value}
   *
   * @see #find()
   */
  public static final String DEFAULT_BASE_DIR_MARKER_FILE_PATH = ".ratpack";

  private BaseDir() {
  }

  /**
   * Finds the “directory” on the classpath that contains a file called {@code .ratpack}.
   * <p>
   * Calling this method is equivalent to calling {@link #find(String) findBaseDir(".ratpack")}.
   *
   * @return a base dir
   * @see #find(String)
   */
  public static Path find() {
    return find(DEFAULT_BASE_DIR_MARKER_FILE_PATH);
  }

  /**
   * Finds the “directory” on the classpath that contains the marker file at the given path.
   * <p>
   * The classpath search is performed using {@link ClassLoader#getResource(String)} using the current thread's {@link Thread#getContextClassLoader() context class loader}.
   * <p>
   * If the resource is not found, an {@link IllegalStateException} will be thrown.
   * <p>
   * If the resource is found, the enclosing directory of the resource will be converted to a {@link Path} and returned.
   * This allows a directory within side a JAR (that is on the classpath) to be used as the base dir potentially.
   *
   * @param markerFilePath the path to the marker file on the classpath
   * @return the base dir
   */
  public static Path find(String markerFilePath) {
    return BaseDirFinder.find(Thread.currentThread().getContextClassLoader(), markerFilePath)
      .map(BaseDirFinder.Result::getBaseDir)
      .orElseThrow(() -> new IllegalStateException("Could not find marker file '" + markerFilePath + "' via context class loader"));
  }

}
