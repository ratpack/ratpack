package org.ratpackframework.handler;

public interface Handler<T> {

  void handle(T event);

}
