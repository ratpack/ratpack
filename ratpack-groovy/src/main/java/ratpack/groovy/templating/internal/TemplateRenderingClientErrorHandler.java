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

package ratpack.groovy.templating.internal;

import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import ratpack.error.ClientErrorHandler;
import ratpack.func.Action;
import ratpack.handling.Context;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static ratpack.util.ExceptionUtils.toException;

public class TemplateRenderingClientErrorHandler implements ClientErrorHandler {

  private final ByteBufAllocator byteBufAllocator;
  private final GroovyTemplateRenderingEngine renderer;

  @Inject
  public TemplateRenderingClientErrorHandler(ByteBufAllocator byteBufAllocator, GroovyTemplateRenderingEngine renderer) {
    this.byteBufAllocator = byteBufAllocator;
    this.renderer = renderer;
  }

  public void error(final Context context, final int statusCode) throws Exception {
    Map<String, Object> model = new HashMap<>();

    HttpResponseStatus status = HttpResponseStatus.valueOf(statusCode);

    model.put("title", status.reasonPhrase());
    model.put("message", status.reasonPhrase());

    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("Request Method", context.getRequest().getMethod().getName());
    metadata.put("Request URL", context.getRequest().getUri());
    model.put("metadata", metadata);

    context.getResponse().status(statusCode).contentType("text/html");

    final ByteBuf byteBuf = byteBufAllocator.buffer();
    renderer.renderError(byteBuf, model)
      .onError(new Action<Throwable>() {
        @Override
        public void execute(Throwable throwable) throws Exception {
          byteBuf.release();
          throw toException(throwable);
        }
      })
      .then(new Action<ByteBuf>() {
        @Override
        public void execute(ByteBuf byteBuf) throws Exception {
          context.getResponse().send(byteBuf);
        }
      });
  }

}
