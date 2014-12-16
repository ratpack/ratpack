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
import ratpack.util.internal.Validations;

public class DefaultPathBinding implements PathBinding {

  private final String binding;
  private final String bindingWithSlash;
  private final String pastBinding;

  private final PathTokens tokens;
  private final PathTokens allTokens;

  public DefaultPathBinding(String path, String binding, ImmutableMap<String, String> tokens, PathBinding parent) {
    this.binding = binding;
    this.bindingWithSlash = binding.concat("/");
    this.tokens = new DefaultPathTokens(tokens);

    if (parent == null) {
      allTokens = new DefaultPathTokens(tokens);
    } else {
      allTokens = new DefaultPathTokens(ImmutableMap.<String, String>builder().putAll(parent.getAllTokens()).putAll(tokens).build());
    }

    if (path.equals(binding)) {
      pastBinding = "";
    } else if (path.startsWith(bindingWithSlash)) {
      pastBinding = path.substring(bindingWithSlash.length());
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

  public String childPath(String path) {
    Validations.noLeadingForwardSlash(path, "child path");
    return bindingWithSlash.concat(path);
  }

  public PathTokens getTokens() {
    return tokens;
  }

  public PathTokens getAllTokens() {
    return allTokens;
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

    return allTokens.equals(that.allTokens)
      && binding.equals(that.binding)
      && bindingWithSlash.equals(that.bindingWithSlash)
      && pastBinding.equals(that.pastBinding)
      && tokens.equals(that.tokens);
  }

  @Override
  public int hashCode() {
    int result = binding.hashCode();
    result = 31 * result + bindingWithSlash.hashCode();
    result = 31 * result + pastBinding.hashCode();
    result = 31 * result + tokens.hashCode();
    result = 31 * result + allTokens.hashCode();
    return result;
  }
}
