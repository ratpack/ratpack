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

package ratpack.server.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;

import static ratpack.util.Exceptions.uncheck;

public abstract class BaseDirFinder {

  public static class Result {
    private final Path baseDir;
    private final Path resource;

    private Result(Path baseDir, Path resource) {
      this.baseDir = baseDir;
      this.resource = resource;
    }

    public Path getBaseDir() {
      return baseDir;
    }

    public Path getResource() {
      return resource;
    }

    @Override
    public String toString() {
      return "Result{baseDir=" + baseDir + ", resource=" + resource + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Result result = (Result) o;

      return baseDir.equals(result.baseDir) && resource.equals(result.resource);
    }

    @Override
    public int hashCode() {
      int result = baseDir.hashCode();
      result = 31 * result + resource.hashCode();
      return result;
    }
  }

  public static Optional<Result> find(ClassLoader classLoader, String resourcePathString) {
    Path resourcePath;
    URL resourceUrl = classLoader.getResource(resourcePathString);
    if (resourceUrl == null) {
      return Optional.empty();
    } else {
      resourcePath = toPath(resourceUrl);
    }

    return Optional.of(new Result(determineBaseDir(resourcePath), resourcePath));
  }

  private static Path toPath(URL resource) {
    URI uri = uncheck(resource::toURI);

    String scheme = uri.getScheme();
    if (scheme.equals("file")) {
      return Paths.get(uri);
    }

    if (!scheme.equals("jar")) {
      throw new IllegalStateException("Cannot deal with class path resource url: " + uri);
    }

    String s = uri.toString();
    int separator = s.indexOf("!/");
    String entryName = s.substring(separator + 2);
    URI fileURI = URI.create(s.substring(0, separator));
    FileSystem fs = null;
    try {
      // Check if there's an existing file system, since it's provider-dependent whether file systems with the same URI are allowed
      fs = FileSystems.getFileSystem(fileURI);
      if (!fs.isOpen()) { // It's provider-dependent whether to return closed file systems
        fs = null; // Ignore it; closed file systems can't be used
      }
    } catch (FileSystemNotFoundException ignore) {
      // Continue to create the file system
    }
    if (fs == null) {
      try {
        fs = FileSystems.newFileSystem(fileURI, ImmutableMap.of());
      } catch (IOException e) {
        throw uncheck(e);
      }
    }
    return fs.getPath(entryName);
  }

  private static Path determineBaseDir(Path configPath) {
    Path baseDir = configPath.getParent();
    if (baseDir == null && configPath.getFileSystem().provider().getScheme().equals("jar")) {
      baseDir = Iterables.getFirst(configPath.getFileSystem().getRootDirectories(), null);
    }
    if (baseDir == null) {
      throw new IllegalStateException("Cannot determine base dir given config resource: " + configPath);
    }
    return baseDir;
  }

}
