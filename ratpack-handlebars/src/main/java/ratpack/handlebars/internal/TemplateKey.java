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
import com.google.common.base.MoreObjects;

public class TemplateKey {
  private final String identifier;
  private final TemplateSource source;

  public TemplateKey(TemplateSource source, boolean reloadable) {
    this.source = source;
    this.identifier = source.filename() + (reloadable ? source.lastModified() : 0);
  }

  public String getIdentifier() {
    return identifier;
  }

  public TemplateSource getSource() {
    return source;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }

    if(o == null || getClass() != o.getClass()) {
      return false;
    }

    TemplateKey that = (TemplateKey) o;

    if(!identifier.equals(that.identifier)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("identifier", identifier)
      .add("source", source)
      .toString();
  }
}
