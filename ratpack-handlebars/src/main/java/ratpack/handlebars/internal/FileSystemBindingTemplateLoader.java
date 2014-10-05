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

import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import ratpack.file.FileSystemBinding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemBindingTemplateLoader extends AbstractTemplateLoader {

  private final FileSystemBinding fileSystemBinding;

  public FileSystemBindingTemplateLoader(FileSystemBinding fileSystemBinding, String suffix) {
    this.fileSystemBinding = fileSystemBinding;
    setSuffix(suffix);
  }

  @Override
  public TemplateSource sourceAt(String location) throws IOException {
    String resolved = resolve(location);
    Path path = fileSystemBinding.file(resolved);
    if (path == null || !Files.exists(path)) {
      throw new IOException("No template at " + resolved + " for binding " + fileSystemBinding);
    } else {
      return new PathTemplateSource(path, fileSystemBinding.getFile());
    }
  }

}
