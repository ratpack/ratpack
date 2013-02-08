package org.ratpackframework;

public interface Handler<T> {

  void handle(T event);

}
