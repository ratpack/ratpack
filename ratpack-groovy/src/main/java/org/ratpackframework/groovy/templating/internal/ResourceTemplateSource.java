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
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.ratpackframework.util.internal.IoUtils;

import java.io.IOException;

class ResourceTemplateSource implements TemplateSource {
  private final String resourcePath;

  ResourceTemplateSource(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  public String getName() {
    return resourcePath;
  }

  public ByteBuf getContent() throws IOException {
    return IoUtils.byteBuf(IOGroovyMethods.getBytes(getClass().getResourceAsStream(resourcePath)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResourceTemplateSource that = (ResourceTemplateSource) o;

    if (!resourcePath.equals(that.resourcePath)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return resourcePath.hashCode();
  }
}
