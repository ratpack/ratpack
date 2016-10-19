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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.reflect.TypeToken;
import ratpack.config.ConfigData;
import ratpack.config.ConfigObject;

public class DelegatingConfigData implements ConfigData {
  private final ConfigData delegate;

  public DelegatingConfigData(ConfigData delegate) {
    this.delegate = delegate;
  }

  @Override
  public ObjectNode getRootNode() {
    return delegate.getRootNode();
  }

  @Override
  public <O> O get(String pointer, Class<O> type) {
    return delegate.get(pointer, type);
  }

  @Override
  public <O> ConfigObject<O> getAsConfigObject(String pointer, TypeToken<O> type) {
    return delegate.getAsConfigObject(pointer, type);
  }

  @Override
  public <O> O get(Class<O> type) {
    return delegate.get(type);
  }
}
