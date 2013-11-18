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

import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.util.Action;

import java.io.PrintWriter;
import java.util.Map;

public class TemplateRenderingServerErrorHandler implements ServerErrorHandler {

  public void error(final Context context, final Exception exception) {
    GroovyTemplateRenderingEngine renderer = context.get(GroovyTemplateRenderingEngine.class);
    Map<String, ?> model = ExceptionToTemplateModel.transform(context.getRequest(), exception);

    Response response = context.getResponse();
    if (response.getStatus().getCode() < 400) {
      response.status(500);
    }

    renderer.renderError(context.getResponse().getBody(), model, new ErrorTemplateRenderResultAction(context, new Action<PrintWriter>() {
      public void execute(PrintWriter writer) {
        writer.append("for server error\n");
        exception.printStackTrace(writer);
      }
    }));
  }

}
