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

import com.github.jknack.handlebars.io.TemplateSource;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathTemplateSource implements TemplateSource {

  private final Path path;
  private final Path bindingPath;

  public PathTemplateSource(Path path, Path bindingPath) {
    this.path = path;
    this.bindingPath = bindingPath;
  }

  @Override
  public String content() throws IOException {
    return new String(Files.readAllBytes(path), CharsetUtil.UTF_8);
  }

  @Override
  public String filename() {
    return bindingPath.relativize(path).toString();
  }

  @Override
  public long lastModified() {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      return -1;
    }
  }
}
