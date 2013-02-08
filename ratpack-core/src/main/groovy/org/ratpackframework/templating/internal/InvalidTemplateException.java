package org.ratpackframework.templating.internal;

import org.ratpackframework.error.ContextualException;

public class InvalidTemplateException extends RuntimeException implements ContextualException {

  public InvalidTemplateException(String templateName, String message, Exception e) {
    super(String.format("[%s] %s", templateName, message), e);
  }

}
