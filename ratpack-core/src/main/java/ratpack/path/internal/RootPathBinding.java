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

package ratpack.path.internal;

import ratpack.path.PathBinding;
import ratpack.path.PathTokens;

public class RootPathBinding implements PathBinding {

  private final String path;

  public RootPathBinding(String path) {
    this.path = path;
  }

  @Override
  public String getBoundTo() {
    return "";
  }

  @Override
  public String getPastBinding() {
    return path;
  }

  @Override
  public PathTokens getTokens() {
    return DefaultPathTokens.empty();
  }

  @Override
  public PathTokens getAllTokens() {
    return DefaultPathTokens.empty();
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RootPathBinding that = (RootPathBinding) o;

    return path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }


}
