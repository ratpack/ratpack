package org.ratpackframework.groovy.templating.internal;

import org.codehaus.groovy.runtime.StackTraceUtils;
import org.ratpackframework.error.ContextualException;
import org.ratpackframework.http.Request;

import java.util.LinkedHashMap;
import java.util.Map;

// TODO is there a better home for this?
public class ExceptionToTemplateModel {

  public static Map<String, ?> transform(Request request, Exception exception) {
    StackTrace stackTrace = decodeStackTrace(exception);
    Map<String, Object> model = new LinkedHashMap<>();
    model.put("title", exception.getClass().getName());
    model.put("message", exception.getMessage());
    model.put("stacktrace", stackTrace.html);

    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("Request Method", request.getMethod().getName());
    metadata.put("Request URL", request.getUri());
    metadata.put("Exception Type", exception.getClass().getName());
    metadata.put("Exception Location", stackTrace.rootCause.getFileName() + ", line " + stackTrace.rootCause.getLineNumber());
    model.put("metadata", metadata);

    return model;
  }

  private static class StackTrace {
    final StringBuilder html;
    StackTraceElement rootCause;

    StackTrace(StringBuilder html, StackTraceElement rootCause) {
      this.html = html;
      this.rootCause = rootCause;
    }
  }

  private static StackTrace decodeStackTrace(Exception exception) {
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
