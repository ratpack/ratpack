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

package ratpack.jackson.internal;

import com.fasterxml.jackson.databind.ObjectWriter;
import ratpack.api.Nullable;
import ratpack.jackson.JsonRender;

public class DefaultJsonRender implements JsonRender {

  private final Object object;
  private final ObjectWriter objectWriter;
  private final Class<?> viewClass;

  public DefaultJsonRender(Object object, @Nullable ObjectWriter objectWriter) {
    this(object, objectWriter, null);
  }

  public DefaultJsonRender(Object object, @Nullable Class<?> viewClass) {
    this(object, null, viewClass);
  }

  public DefaultJsonRender(Object object, @Nullable ObjectWriter objectWriter, @Nullable Class<?> viewClass) {
    this.object = object;
    this.objectWriter = objectWriter;
    this.viewClass = viewClass;
  }

  @Override
  public Object getObject() {
    return object;
  }

  @Override
  public ObjectWriter getObjectWriter() {
    return objectWriter;
  }

  @Override
  public Class<?> getViewClass() {
    return viewClass;
  }
}
