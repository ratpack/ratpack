/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.handler;

import org.codehaus.groovy.runtime.StackTraceUtils;
import org.ratpackframework.templating.CompiledTemplate;
import org.ratpackframework.templating.TemplateCompiler;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class ErrorHandler implements Handler<ErroredHttpServerRequest> {

  private final TemplateCompiler templateCompiler;

  public ErrorHandler(TemplateCompiler templateCompiler) {
    this.templateCompiler = templateCompiler;
  }

  @Override
  public void handle(ErroredHttpServerRequest erroredRequest) {
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    Exception error = (Exception) StackTraceUtils.deepSanitize(erroredRequest.getException());
    HttpServerRequest request = erroredRequest.getRequest();
    request.response.statusCode = 500;
    renderException(error, request, new FallbackErrorHandlingTemplateRenderer(request, "rendering error template"));
  }

  public <T> AsyncResultHandler<T> wrap(final HttpServerRequest request, final Handler<T> handler) {
    return new AsyncResultHandler<T>() {
      @Override
      public void handle(AsyncResult<T> event) {
        if (event.failed()) {
          ErrorHandler.this.handle(new ErroredHttpServerRequest(request, event.exception));
        } else {
          handler.handle(event.result);
        }
      }
    };
  }

  void renderException(Exception exception, HttpServerRequest request, AsyncResultHandler<CompiledTemplate> handler) {
    DecodedStackTrace decodedStackTrace = decodeStackTrace(exception);
    Map<Object, Object> model = new LinkedHashMap<Object, Object>();
    model.put("title", exception.getClass().getName());
    model.put("message", exception.getMessage());
    model.put("stacktrace", decodedStackTrace.html);

    Map<Object, Object> metadata = new LinkedHashMap<Object, Object>();
    metadata.put("Request Method", request.method.toUpperCase());
    metadata.put("Request URL", request.uri);
    metadata.put("Exception Type", exception.getClass().getName());
    metadata.put("Exception Location", decodedStackTrace.rootCause.getFileName() + ", line " + decodedStackTrace.rootCause.getLineNumber());
    model.put("metadata", metadata);

    templateCompiler.compileErrorTemplate(model, handler);
  }

  private static class DecodedStackTrace {
    final String html;
    final StackTraceElement rootCause;

    DecodedStackTrace(String html, StackTraceElement rootCause) {
      this.html = html;
      this.rootCause = rootCause;
    }
  }

  protected static DecodedStackTrace decodeStackTrace(Exception exception) {
    StringBuilder html = new StringBuilder();
    html.append(exception.toString()).append("\n");
    StackTraceElement rootCause = null;

    for (StackTraceElement ste : exception.getStackTrace()) {
      if (!StackTraceUtils.isApplicationClass(ste.getClassName())) {
        html.append("<span class='stack-thirdparty'>        at ").append(ste.toString()).append("</span>\n");
      } else {
        html.append("        at ").append(ste.toString()).append("\n");
        if (rootCause == null) {
          rootCause = ste;
        }
      }
    }

    return new DecodedStackTrace(html.toString(), rootCause);
  }


}


