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

package ratpack.file.internal;

import ratpack.file.FileSystemBinding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultFileSystemBinding implements FileSystemBinding {

  private final Path binding;
  private final Path dummyNonRootBinding;

  public DefaultFileSystemBinding(Path binding) {
    if (!binding.isAbsolute()) {
      throw new IllegalArgumentException("Filesystem binding must be absolute");
    }
    this.binding = binding;

    if (binding.toString().equals("/")) {
      dummyNonRootBinding = binding.resolve("dummy");
    } else {
      dummyNonRootBinding = null;
    }
  }

  public Path getFile() {
    return binding;
  }

  public Path file(String path) {
    if (path == null) {
      throw new IllegalArgumentException("Path must not be null");
    }

    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    if (dummyNonRootBinding == null) {
      Path child = binding.resolve(path).normalize();
      if (child.startsWith(binding)) {
        return child;
      } else {
        return null;
      }
    } else {
      Path child = dummyNonRootBinding.resolve(path).normalize();
      if (child.startsWith(dummyNonRootBinding)) {
        return binding.resolve(path).normalize();
      } else {
        return null;
      }
    }
  }

  public FileSystemBinding binding(String path) {
    Path file = file(path);
    if (file != null) {
      return new DefaultFileSystemBinding(file);
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "FileSystemBinding[" + binding + ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultFileSystemBinding that = (DefaultFileSystemBinding) o;

    try {
      return Files.isSameFile(binding, that.binding);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int hashCode() {
    return binding.hashCode();
  }
}
