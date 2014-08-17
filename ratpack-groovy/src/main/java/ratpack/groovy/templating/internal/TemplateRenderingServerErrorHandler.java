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
import ratpack.error.ServerErrorHandler;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.http.Response;

import java.util.Map;

import static ratpack.util.ExceptionUtils.toException;

public class TemplateRenderingServerErrorHandler implements ServerErrorHandler {

  private final ByteBufAllocator byteBufAllocator;
  private final GroovyTemplateRenderingEngine renderer;

  @Inject
  public TemplateRenderingServerErrorHandler(ByteBufAllocator byteBufAllocator, GroovyTemplateRenderingEngine renderer) {
    this.byteBufAllocator = byteBufAllocator;
    this.renderer = renderer;
  }

  public void error(final Context context, final Throwable throwable) throws Exception {
    Map<String, ?> model = ExceptionToTemplateModel.transform(context.getRequest(), throwable);

    Response response = context.getResponse();
    if (response.getStatus().getCode() < 400) {
      response.status(500);
    }

    final ByteBuf byteBuf = byteBufAllocator.buffer();
    renderer.renderError(byteBuf, model).onError(new Action<Throwable>() {
      @Override
      public void execute(Throwable thing) throws Exception {
        byteBuf.release();
        throw toException(thing);
      }
    }).then(new Action<ByteBuf>() {
      @Override
      public void execute(ByteBuf byteBuf) throws Exception {
        context.getResponse().send(byteBuf);
      }
    });
  }

}
