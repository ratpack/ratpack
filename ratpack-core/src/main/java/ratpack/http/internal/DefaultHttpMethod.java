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

package ratpack.http.internal;

import ratpack.http.HttpMethod;

public class DefaultHttpMethod implements HttpMethod {

  private final String name;

  public DefaultHttpMethod(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public boolean isPost() {
    return name.equals("POST");
  }

  public boolean isGet() {
    return name.equals("GET") || name.equals("HEAD");
  }

  public boolean isHead() {
    return name.equals("HEAD");
  }

  public boolean isPut() {
    return name.equals("PUT");
  }

  public boolean isPatch() {
    return name.equals("PATCH");
  }  

  public boolean isDelete() {
    return name.equals("DELETE");
  }

  public boolean isOptions() {
    return name.equals("OPTIONS");
  }

  public boolean name(String name) {
    if (name.equalsIgnoreCase("GET")) {
      return isGet();
    } else {
      return this.name.equalsIgnoreCase(name);
    }
  }

}
