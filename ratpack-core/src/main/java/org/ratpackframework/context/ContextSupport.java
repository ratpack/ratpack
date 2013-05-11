package org.ratpackframework.context;

public abstract class ContextSupport implements Context {

  private final Context parent;

  protected ContextSupport(Context parent) {
    this.parent = parent;
  }

  public abstract <T> T doMaybeGet(Class<T> type);

  @Override
  public <T> T maybeGet(Class<T> type) {
    T value = doMaybeGet(type);

    if (value == null) {
      return parent.maybeGet(type);
    } else {
      return value;
    }
  }

  @Override
  public <T> T get(Class<T> type) {
    T found = maybeGet(type);
    if (found == null) {
      throw new IllegalStateException(String.format("Could not find %s in context", type));
    }

    return found;
  }

}
