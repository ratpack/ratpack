/*
 * Copyright 2014 the original author or authors.
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

package ratpack.http.client.internal;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.http.MutableHeaders;
import ratpack.http.client.RequestSpec;
import ratpack.util.internal.ByteBufWriteThroughOutputStream;

import java.io.OutputStream;

public class RequestSpecBacking {

  private final MutableHeaders headers;
  private final ByteBuf body;

  private String method = "GET";

  public RequestSpecBacking(MutableHeaders headers, ByteBuf body) {
    this.headers = headers;
    this.body = body;
  }

  public String getMethod() {
    return method;
  }

  @Nullable
  public ByteBuf getBody() {
    return body;
  }

  public RequestSpec asSpec() {
    return new Spec();
  }

  private class Spec implements RequestSpec {
    @Override
    public MutableHeaders getHeaders() {
      return headers;
    }

    @Override
    public RequestSpec method(String method) {
      RequestSpecBacking.this.method = method.toUpperCase();
      return this;
    }

    private class BodyImpl implements Body {
      @Override
      public Body type(String contentType) {
        getHeaders().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        return this;
      }

      @Override
      public Body stream(Action<? super OutputStream> action) throws Exception {
        try (OutputStream outputStream = new ByteBufWriteThroughOutputStream(body.clear())) {
          action.execute(outputStream);
        }

        return this;
      }
    }

    @Override
    public Body getBody() {
      return new BodyImpl();
    }

    @Override
    public RequestSpec body(Action<? super Body> action) throws Exception {
      action.execute(getBody());
      return this;
    }
  }
}