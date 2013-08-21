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

package org.ratpackframework.path;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

public class PathTokens extends AbstractMap<String, String> {

  private final Map<String, String> delegate;

  public PathTokens(Map<String, String> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return delegate.entrySet();
  }
}
