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

import java.io.Closeable;
import java.nio.file.Path;

public interface BaseDirBuilder extends Closeable {

  /**
   * Returns a path for the given path within the base dir.
   * <p>
   * All parent directories will be created on demand.
   *
   * @param path The relative path to the path
   */
  Path file(String path);

  /**
   * Creates a file with the given string content at the given path within the base dir.
   * <p>
   * All parent directories will be created on demand.
   *
   * @param path The relative path to the file to create
   * @param content The content to write to the file
   */
  Path file(String path, String content);

  /**
   * Creates a directory at the given path within the base dir.
   * <p>
   * All parent directories will be created on demand.
   *
   * @param path The relative path to the file to create
   */
  Path dir(String path);

  /**
   * Build the baseDir.
   *
   * @return The base dir
   */
  Path build();

}
