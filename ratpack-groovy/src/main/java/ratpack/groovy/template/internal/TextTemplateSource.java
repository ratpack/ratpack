/*
 * Copyright 2014 the original author or authors.
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

package ratpack.groovy.template.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ratpack.util.internal.IoUtils;

import java.io.IOException;
import java.nio.file.Path;

public class TextTemplateSource {

  private final String id;
  private final ByteBufAllocator byteBufAllocator;
  private final Path path;
  private final String name;

  public TextTemplateSource(ByteBufAllocator byteBufAllocator, String id, Path path, String name) {
    this.byteBufAllocator = byteBufAllocator;
    this.path = path;
    this.name = name;
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public ByteBuf getContent() throws IOException {
    return IoUtils.read(byteBufAllocator, path);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TextTemplateSource that = (TextTemplateSource) o;

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
