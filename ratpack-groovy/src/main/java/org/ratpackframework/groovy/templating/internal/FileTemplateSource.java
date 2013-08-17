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

package org.ratpackframework.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import org.ratpackframework.util.internal.IoUtils;

import java.io.File;
import java.io.IOException;

class FileTemplateSource implements TemplateSource {

  private final File file;
  private final String name;
  private final String id;

  public FileTemplateSource(File file, String name, boolean reloadable) {
    this.file = file;
    this.name = name;
    this.id = file.getAbsolutePath() + File.separator + (reloadable ? file.lastModified() : 0);
  }

  public String getName() {
    return name;
  }

  public ByteBuf getContent() throws IOException {
    return IoUtils.readFile(file);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FileTemplateSource that = (FileTemplateSource) o;

    if (!id.equals(that.id)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
