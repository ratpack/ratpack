package org.ratpackframework.context;

public interface Context {

  <T> T get(Class<T> type);

  <T> T require(Class<T> type);

  Context push(Object value);

}
