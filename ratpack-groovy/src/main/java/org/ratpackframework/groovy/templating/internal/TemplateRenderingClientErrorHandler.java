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
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.error.ClientErrorHandler;
import org.ratpackframework.handling.Context;
import org.ratpackframework.http.Response;
import org.ratpackframework.util.Action;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateRenderingClientErrorHandler implements ClientErrorHandler {

  public void error(final Context context, final int statusCode) {
    GroovyTemplateRenderingEngine renderer = context.get(GroovyTemplateRenderingEngine.class);
    Map<String, Object> model = new HashMap<>();

    HttpResponseStatus status = HttpResponseStatus.valueOf(statusCode);

    model.put("title", status.reasonPhrase());
    model.put("message", status.reasonPhrase());

    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("Request Method", context.getRequest().getMethod().getName());
    metadata.put("Request URL", context.getRequest().getUri());
    model.put("metadata", metadata);

    context.getResponse().status(statusCode);

    renderer.renderError(context.getResponse().getBody(), model, context.resultAction(new Action<ByteBuf>() {
      public void execute(ByteBuf byteBuf) {
        Response response = context.getResponse();
        response.status(statusCode).contentType("text/html").send();
      }
    }));
  }

}
