package org.ratpackframework;

/**
 * A generic type for an object that does some work with a thing.
 *
 * @param <T> The type of thing.
 */
public interface Handler<T> {

  void handle(T event);

}
