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

package org.ratpackframework.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.handling.Context;
import org.ratpackframework.http.Response;
import org.ratpackframework.util.internal.ReleasingAction;

import java.util.Map;

public class TemplateRenderingServerErrorHandler implements ServerErrorHandler {

  public void error(final Context context, final Exception exception) {
    GroovyTemplateRenderingEngine renderer = context.get(GroovyTemplateRenderingEngine.class);
    Map<String, ?> model = ExceptionToTemplateModel.transform(context.getRequest(), exception);
    renderer.renderError(model, context.resultAction(new ReleasingAction<ByteBuf>() {
      protected void doExecute(ByteBuf byteBuf) {
        Response response = context.getResponse();
        if (response.getStatus().getCode() < 400) {
          response.status(500);
        }
        response.send("text/html", byteBuf);
      }
    }));
  }

}
