package org.ratpackframework.context.internal;

import org.ratpackframework.context.Context;
import org.ratpackframework.context.ContextSupport;

public class ObjectHoldingContext extends ContextSupport {

  private final Object value;

  public ObjectHoldingContext(Context parent, Object value) {
    super(parent);
    this.value = value;
  }

  @Override
  public <T> T doGet(Class<T> type) {
    if (type.isInstance(value)) {
      return type.cast(value);
    } else {
      return null;
    }
  }

}
