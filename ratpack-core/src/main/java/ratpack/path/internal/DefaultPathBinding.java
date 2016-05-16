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

package ratpack.path.internal;

import com.google.common.collect.ImmutableMap;
import ratpack.path.PathBinding;
import ratpack.path.PathTokens;

public class DefaultPathBinding implements PathBinding {

  private final String binding;
  private final String pastBinding;
  private final String description;

  private final PathTokens tokens;
  private final PathTokens allTokens;

  public DefaultPathBinding(String binding, ImmutableMap<String, String> tokens, PathBinding parent, String description) {
    this.binding = binding;
    this.description = parent instanceof RootPathBinding ? description : parent.getSpec() + "/" + description;
    this.tokens = DefaultPathTokens.of(tokens);
    this.allTokens = parent.getAllTokens().isEmpty() ? this.tokens : DefaultPathTokens.of(ImmutableMap.<String, String>builder().putAll(parent.getAllTokens()).putAll(tokens).build());

    String bindingWithSlash = binding.concat("/");
    String path = parent.getPastBinding();
    if (path.equals(binding)) {
      pastBinding = "";
    } else if (path.startsWith(bindingWithSlash)) {
      pastBinding = path.substring(bindingWithSlash.length());
    } else {
      throw new IllegalArgumentException(String.format("Path '%s' is not a child of '%s'", path, binding));
    }
  }

  @Override
  public String getSpec() {
    return description;
  }

  @Override
  public String getPastBinding() {
    return pastBinding;
  }

  @Override
  public String getBoundTo() {
    return binding;
  }

  @Override
  public PathTokens getTokens() {
    return tokens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultPathBinding that = (DefaultPathBinding) o;

    return binding.equals(that.binding)
      && pastBinding.equals(that.pastBinding)
      && description.equals(that.description)
      && allTokens.equals(that.allTokens);
  }

  @Override
  public int hashCode() {
    int result = binding.hashCode();
    result = 31 * result + pastBinding.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + allTokens.hashCode();
    return result;
  }

  @Override
  public PathTokens getAllTokens() {
    return allTokens;
  }
}
