package com.bleedingwolf.ratpack.internal

interface MaybeHandler<T> {

  boolean maybeHandle(T event)

}
