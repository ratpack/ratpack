package org.ratpackframework;

import com.google.inject.Injector;
import com.google.inject.Key;

public class DefaultObjects implements Objects {

  private final Injector injector;

  public DefaultObjects(Injector injector) {
    this.injector = injector;
  }

  @Override
  public Injector getInjector() {
    return injector;
  }

  @Override
  public <T> T get(Class<T> type) {
    return injector.getInstance(type);
  }

  @Override
  public <T> T get(Key<T> key) {
    return injector.getInstance(key);
  }

}
