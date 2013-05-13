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

package org.ratpackframework.path.internal;

import org.ratpackframework.path.PathContext;
import org.ratpackframework.util.internal.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultPathContext implements PathContext {

  private final String binding;
  private final String bindingTerminated;
  private final String pastBinding;

  private final Map<String, String> tokens;
  private final Map<String, String> allTokens;
  private final PathContext parent;

  public DefaultPathContext(String path, String binding, Map<String, String> tokens, PathContext parent) {
    this.binding = binding;
    this.tokens = tokens;
    this.parent = parent;

    if (parent == null) {
      allTokens = tokens;
    } else {
      allTokens = new LinkedHashMap<String, String>(parent.getAllTokens());
      allTokens.putAll(tokens);
    }

    this.bindingTerminated = binding + "/";

    if (path.equals(binding)) {
      pastBinding = "";
    } else if (path.startsWith(bindingTerminated)) {
      pastBinding = path.substring(bindingTerminated.length());
    } else {
      throw new IllegalArgumentException(String.format("Path '%s' is not a child of '%s'", path, binding));
    }
  }

  public String getPastBinding() {
    return pastBinding;
  }

  public String getBoundTo() {
    return binding;
  }

  public String join(String path) {
    return CollectionUtils.join("/", this.binding, path);
  }

  public Map<String, String> getTokens() {
    return tokens;
  }

  public Map<String, String> getAllTokens() {
    return allTokens;
  }
}
