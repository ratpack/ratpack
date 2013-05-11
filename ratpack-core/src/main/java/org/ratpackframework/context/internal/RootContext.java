package org.ratpackframework.context.internal;

import org.ratpackframework.context.Context;

public class RootContext implements Context {

  @Override
  public <T> T get(Class<T> type) {
    return null;
  }

  @Override
  public <T> T require(Class<T> type) {
    throw new IllegalStateException(String.format("Could not find %s in context", type));
  }
}
