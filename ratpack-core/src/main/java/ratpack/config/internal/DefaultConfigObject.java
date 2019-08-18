/*
 * Copyright 2015 the original author or authors.
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

package ratpack.config.internal;

import com.google.common.reflect.TypeToken;
import ratpack.config.ConfigObject;
import ratpack.util.Types;

public class DefaultConfigObject<T> implements ConfigObject<T> {

  private final String path;
  private final TypeToken<T> type;
  private final T object;

  public DefaultConfigObject(String path, TypeToken<T> type, T object) {
    this.path = path;
    this.type = type;
    this.object = object;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public Class<T> getType() {
    return Types.cast(type.getRawType());
  }

  @Override
  public TypeToken<T> getTypeToken() {
    return type;
  }

  @Override
  public T getObject() {
    return object;
  }
}
