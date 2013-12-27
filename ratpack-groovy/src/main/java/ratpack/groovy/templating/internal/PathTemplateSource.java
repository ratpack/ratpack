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

package ratpack.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import ratpack.util.internal.IoUtils;

import java.io.IOException;
import java.nio.file.Path;

class PathTemplateSource implements TemplateSource {

  private final String id;
  private final Path path;
  private final String name;

  public PathTemplateSource(String id, Path path, String name) {
    this.path = path;
    this.name = name;
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public ByteBuf getContent() throws IOException {
    return IoUtils.read(path);
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

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
