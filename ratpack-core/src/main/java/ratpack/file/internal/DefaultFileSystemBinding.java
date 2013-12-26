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

import java.io.File;
import java.nio.file.Paths;

public class DefaultFileSystemBinding implements FileSystemBinding {

  private final File file;

  public DefaultFileSystemBinding(File file) {
    this.file = file;
  }

  public File getFile() {
    return file;
  }

  public File file(String path) {
    if (inRoot(path)) {
      return new File(file, path);
    } else {
      return null;
    }
  }

  public FileSystemBinding binding(String path) {
    File binding = file(path);
    if (binding != null) {
      return new DefaultFileSystemBinding(binding);
    } else {
      return null;
    }
  }

  public FileSystemBinding binding(File file) {
    return new DefaultFileSystemBinding(file);
  }

  private boolean inRoot(String path) {
    String root = file.getAbsolutePath();
    return Paths.get(root, path).toAbsolutePath().normalize().startsWith(root);
  }
}
