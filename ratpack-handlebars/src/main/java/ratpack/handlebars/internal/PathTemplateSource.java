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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathTemplateSource implements TemplateSource {

  private final Path path;
  private final String filename;
  private final long lastModified;

  public PathTemplateSource(Path path, Path bindingPath) {
    this.path = path;
    this.filename = bindingPath.relativize(path).toString();

    long lastModified1;
    try {
      lastModified1 = Files.getLastModifiedTime(path).toMillis();
    } catch (IOException e) {
      lastModified1 = -1;
    }
    this.lastModified = lastModified1;
  }

  @Override
  public String content(Charset charset) throws IOException {
    return new String(Files.readAllBytes(path), charset);
  }

  @Override
  public String filename() {
    return filename;
  }

  @Override
  public long lastModified() {
    return lastModified;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PathTemplateSource that = (PathTemplateSource) o;
    return path.equals(that.path) && filename.equals(that.filename);
  }

  @Override
  public int hashCode() {
    int result = path.hashCode();
    result = 31 * result + filename.hashCode();
    return result;
  }
}
