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

package ratpack.handlebars.internal;

import com.github.jknack.handlebars.io.URLTemplateLoader;
import ratpack.file.FileSystemBinding;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class FileSystemBindingTemplateLoader extends URLTemplateLoader {

  private final FileSystemBinding fileSystemBinding;

  public FileSystemBindingTemplateLoader(FileSystemBinding fileSystemBinding, String suffix) {
    this.fileSystemBinding = fileSystemBinding;
    setSuffix(suffix);
  }

  @Override
  protected URL getResource(String location) throws IOException {
    Path path = fileSystemBinding.file(location);
    if (path == null) {
      throw new IOException("No template at " + location + " for binding " + fileSystemBinding);
    } else {
      return path.toUri().toURL();
    }
  }

}
