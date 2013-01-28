package org.ratpackframework.templating.internal;

public class InvalidTemplateException extends RuntimeException {

  public InvalidTemplateException(String templateName, String message) {
    super(String.format("[%s] %s", templateName, message));
  }

}
