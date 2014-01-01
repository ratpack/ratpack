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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * A {@link BaseDirBuilder} that is based on an initial {@link Path}.
 */
public class PathBaseDirBuilder implements BaseDirBuilder {

  private final Path baseDir;

  /**
   * Constructor.
   *
   * @param baseDir used to call {@link #PathBaseDirBuilder(java.nio.file.Path) this(baseDir.toPath())}
   */
  public PathBaseDirBuilder(File baseDir) {
    this(baseDir.toPath());
  }

  /**
   * Constructor.
   *
   * @param baseDir The base dir for this builder (will be returned by {@link #build()}
   */
  public PathBaseDirBuilder(Path baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path file(String path) {
    return toUsablePath(path);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path file(String path, String content) {
    Path file = file(path);
    try {
      Files.write(file, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      // zip file system doesn't update on write over existing file
      Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
    } catch (IOException e) {
      throw uncheck(e);
    }
    return file;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path dir(String path) {
    Path file = toUsablePath(path);
    try {
      Files.createDirectory(file);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return file;
  }

  /**
   * If the path backing this build is not from the default file system, its file system will be {@linkplain java.nio.file.FileSystem#close() closed}.
   *
   * @throws IOException If the build could not be closed cleanly
   */
  @Override
  public void close() throws IOException {
    FileSystem fileSystem = baseDir.getFileSystem();
    if (!fileSystem.equals(FileSystems.getDefault())) {
      fileSystem.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path build() {
    return baseDir;
  }

  private Path toUsablePath(String path) {
    Path p = baseDir.resolve(path);
    try {
      Files.createDirectories(p.getParent());
    } catch (IOException e) {
      throw uncheck(e);
    }
    return p;
  }
}
