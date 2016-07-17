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

import com.google.common.collect.ImmutableMap;
import ratpack.http.HttpMethod;

import java.util.IdentityHashMap;
import java.util.Map;

public class DefaultHttpMethod implements HttpMethod {

  private final io.netty.handler.codec.http.HttpMethod nettyMethod;

  private static final Map<io.netty.handler.codec.http.HttpMethod, HttpMethod> METHODS = new IdentityHashMap<>(
    ImmutableMap.<io.netty.handler.codec.http.HttpMethod, HttpMethod>builder()
      .put(io.netty.handler.codec.http.HttpMethod.GET, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.GET))
      .put(io.netty.handler.codec.http.HttpMethod.HEAD, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.HEAD))
      .put(io.netty.handler.codec.http.HttpMethod.POST, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.POST))
      .put(io.netty.handler.codec.http.HttpMethod.PUT, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.PUT))
      .put(io.netty.handler.codec.http.HttpMethod.DELETE, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.DELETE))
      .put(io.netty.handler.codec.http.HttpMethod.OPTIONS, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.OPTIONS))
      .put(io.netty.handler.codec.http.HttpMethod.PATCH, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.PATCH))
      .put(io.netty.handler.codec.http.HttpMethod.TRACE, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.TRACE))
      .put(io.netty.handler.codec.http.HttpMethod.CONNECT, new DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod.CONNECT))
      .build()
  );

  private DefaultHttpMethod(io.netty.handler.codec.http.HttpMethod nettyMethod) {
    this.nettyMethod = nettyMethod;
  }

  public String getName() {
    return nettyMethod.name();
  }

  public boolean isPost() {
    return nettyMethod == io.netty.handler.codec.http.HttpMethod.POST;
  }

  public boolean isGet() {
    return nettyMethod == io.netty.handler.codec.http.HttpMethod.GET || nettyMethod == io.netty.handler.codec.http.HttpMethod.HEAD;
  }

  public boolean isHead() {
    return nettyMethod == io.netty.handler.codec.http.HttpMethod.HEAD;
  }

  public boolean isPut() {
    return nettyMethod == io.netty.handler.codec.http.HttpMethod.PUT;
  }

  public boolean isPatch() {
    return nettyMethod == io.netty.handler.codec.http.HttpMethod.PATCH;
  }

  public boolean isDelete() {
    return nettyMethod == io.netty.handler.codec.http.HttpMethod.DELETE;
  }

  public boolean isOptions() {
    return nettyMethod == io.netty.handler.codec.http.HttpMethod.OPTIONS;
  }

  public boolean name(String name) {
    if (name.equalsIgnoreCase("GET")) {
      return isGet();
    } else {
      return this.nettyMethod.name().equalsIgnoreCase(name);
    }
  }

  @Override
  public io.netty.handler.codec.http.HttpMethod getNettyMethod() {
    return this.nettyMethod;
  }

  @Override
  public String toString() {
    return this.nettyMethod.name().toUpperCase();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultHttpMethod that = (DefaultHttpMethod) o;

    return nettyMethod.equals(that.nettyMethod);
  }

  @Override
  public int hashCode() {
    return nettyMethod.hashCode();
  }

  public static HttpMethod valueOf(io.netty.handler.codec.http.HttpMethod method) {
    HttpMethod httpMethod = METHODS.get(method);
    return httpMethod == null ? new DefaultHttpMethod(method) : httpMethod;
  }

}
