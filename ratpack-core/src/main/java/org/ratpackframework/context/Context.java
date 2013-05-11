package org.ratpackframework.context;

public interface Context {

  <T> T get(Class<T> type);

  <T> T maybeGet(Class<T> type);

}
