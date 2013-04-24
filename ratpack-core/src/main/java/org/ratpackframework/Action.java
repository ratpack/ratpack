package org.ratpackframework;

/**
 * A generic type for an object that does some work with a thing.
 *
 * @param <T> The type of thing.
 */
public interface Action<T> {

  void execute(T event);

}
