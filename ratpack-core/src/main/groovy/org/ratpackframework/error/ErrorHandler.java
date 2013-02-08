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

package org.ratpackframework.error;

import org.codehaus.groovy.runtime.StackTraceUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.ratpackframework.Handler;
import org.ratpackframework.handler.HttpExchange;
import org.ratpackframework.handler.ResultHandler;
import org.ratpackframework.templating.TemplateRenderer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ErrorHandler implements Handler<ErroredHttpExchange> {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final TemplateRenderer templateRenderer;

  @Inject
  public ErrorHandler(TemplateRenderer templateRenderer) {
    this.templateRenderer = templateRenderer;
  }

  @Override
  public void handle(ErroredHttpExchange erroredRequest) {
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    Exception error = (Exception) StackTraceUtils.deepSanitize(erroredRequest.getException());
    logger.log(Level.WARNING, "error handling " + erroredRequest.getExchange().getRequest().getUri(), error);
    HttpExchange exchange = erroredRequest.getExchange();
    HttpRequest request = exchange.getRequest();
    exchange.getResponse().setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    renderException(error, request, new FallbackErrorHandlingTemplateRenderer(exchange, "rendering error template"));
  }

  void renderException(Exception exception, HttpRequest request, ResultHandler<ChannelBuffer> handler) {
    StackTrace stackTrace = decodeStackTrace(exception);
    Map<String, Object> model = new LinkedHashMap<String, Object>();
    model.put("title", exception.getClass().getName());
    model.put("message", exception.getMessage());
    model.put("stacktrace", stackTrace.html);

    Map<String, Object> metadata = new LinkedHashMap<String, Object>();
    metadata.put("Request Method", request.getMethod().getName());
    metadata.put("Request URL", request.getUri());
    metadata.put("Exception Type", exception.getClass().getName());
    metadata.put("Exception Location", stackTrace.rootCause.getFileName() + ", line " + stackTrace.rootCause.getLineNumber());
    model.put("metadata", metadata);

    templateRenderer.renderError(model, handler);
  }

  private static class StackTrace {
    final StringBuilder html;
    StackTraceElement rootCause;

    StackTrace(StringBuilder html, StackTraceElement rootCause) {
      this.html = html;
      this.rootCause = rootCause;
    }
  }

  protected static StackTrace decodeStackTrace(Exception exception) {
    StackTrace trace = new StackTrace(new StringBuilder(), null);
    trace.html.append(exception.toString()).append("\n");
    renderFrames(exception, trace);
    return trace;
  }

  private static void renderFrames(Throwable exception, StackTrace trace) {
    for (StackTraceElement ste : exception.getStackTrace()) {
      if (!StackTraceUtils.isApplicationClass(ste.getClassName())) {
        trace.html.append("<span class='stack-thirdparty'>  at ").append(ste.toString()).append("</span>\n");
      } else {
        trace.html.append("  at ").append(ste.toString()).append("\n");
        if (trace.rootCause == null && !ContextualException.class.isInstance(exception)) {
          trace.rootCause = ste;
        }
      }
    }

    Throwable cause = exception.getCause();
    if (cause != null) {
      trace.html.append("Caused by: ").append(cause.toString()).append("\n");
      renderFrames(cause, trace);
    }
  }

}


