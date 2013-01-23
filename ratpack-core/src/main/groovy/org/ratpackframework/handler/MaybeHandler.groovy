package org.ratpackframework.handler

interface MaybeHandler<T> {

  boolean maybeHandle(T event)

}
