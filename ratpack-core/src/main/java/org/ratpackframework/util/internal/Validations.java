package org.ratpackframework.util.internal;

public abstract class Validations {

  public static void noLeadingForwardSlash(String string, String description) {
    if (string.startsWith("/")) {
      throw new IllegalArgumentException(String.format("%s string (value: %s) should not start with a forward slash", description, string));
    }
  }
}
