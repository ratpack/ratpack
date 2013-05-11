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

package org.ratpackframework.http.internal;

import org.ratpackframework.http.HttpMethod;

public class DefaultHttpMethod implements HttpMethod {

  private final String name;

  public DefaultHttpMethod(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isPost() {
    return name.equals("POST");
  }

  @Override
  public boolean isGet() {
    return name.equals("GET");
  }

  @Override
  public boolean isPut() {
    return name.equals("PUT");
  }

  @Override
  public boolean isDelete() {
    return name.equals("DELETE");
  }

  @Override
  public boolean name(String name) {
    return this.name.equals(name.toUpperCase());
  }

}
