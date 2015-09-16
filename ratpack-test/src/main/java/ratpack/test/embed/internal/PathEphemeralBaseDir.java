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

package ratpack.test.embed.internal;

import ratpack.test.embed.EphemeralBaseDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class PathEphemeralBaseDir implements EphemeralBaseDir {

  private final Path baseDir;
  private final Path toDelete;

  public PathEphemeralBaseDir(File baseDir) {
    this(baseDir.toPath());
  }

  public PathEphemeralBaseDir(Path baseDir) {
    this(baseDir, baseDir);
  }

  public PathEphemeralBaseDir(Path baseDir, Path toDelete) {
    this.baseDir = baseDir;
    this.toDelete = toDelete;
  }

  @Override
  public void close() throws IOException {
    FileSystem fileSystem = baseDir.getFileSystem();
    if (!fileSystem.equals(FileSystems.getDefault())) {
      fileSystem.close();
    }

    System.gc(); // this is required for builds to pass on Windows

    if (Files.exists(toDelete)) {
      Files.walkFileTree(toDelete, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  @Override
  public Path getRoot() {
    return baseDir;
  }

}
