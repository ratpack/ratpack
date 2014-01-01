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

import java.nio.file.Path;

public class DefaultFileSystemBinding implements FileSystemBinding {

  private final Path binding;

  public DefaultFileSystemBinding(Path binding) {
    if (!binding.isAbsolute()) {
      throw new IllegalArgumentException("Filesystem binding must be absolute");
    }
    this.binding = binding;
  }

  public Path getFile() {
    return binding;
  }

  public Path file(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    Path child = binding.resolve(path).normalize();
    if (child.startsWith(binding)) {
      return child;
    } else {
      return null;
    }
  }

  public FileSystemBinding binding(String path) {
    Path binding = file(path);
    if (binding != null) {
      return new DefaultFileSystemBinding(binding);
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "FileSystemBinding[" + binding + ']';
  }
}
