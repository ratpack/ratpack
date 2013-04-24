package org.ratpackframework;

import com.google.inject.Injector;
import com.google.inject.Key;

public interface Objects {

  /**
   * The injector that backs this application.
   */
  Injector getInjector();

  /**
   * Retrieves the specified service from the services registered at startup.
   *
   * @param type The type of the service.
   * @param <T> The type of the service.
   * @return The service instance.
   */
  <T> T get(Class<T> type);

  /**
   * Retrieves the specified service from the services registered at startup.
   *
   * @param key The key of the service.
   * @param <T> The type of the service.
   * @return The service instance.
   */
  <T> T get(Key<T> key);

}
