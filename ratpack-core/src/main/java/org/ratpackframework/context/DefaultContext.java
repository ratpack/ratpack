package org.ratpackframework.context;

public class DefaultContext implements Context {

  private final Context parent;
  private final Object value;

  public DefaultContext() {
    this.parent = null;
    this.value = null;
  }

  private DefaultContext(Context parent, Object value) {
    this.parent = parent;
    this.value = value;
  }

  @Override
  public <T> T get(Class<T> type) {
    if (value == null) {
      return null;
    } else if (type.isInstance(value)) {
      return type.cast(value);
    } else {
      return parent.get(type);
    }
  }

  @Override
  public <T> T require(Class<T> type) {
    T found = get(type);
    if (found == null) {
      throw new IllegalStateException(String.format("Could not find %s in context", type));
    }

    return found;
  }

  @Override
  public Context push(Object value) {
    return new DefaultContext(this, value);
  }

}
