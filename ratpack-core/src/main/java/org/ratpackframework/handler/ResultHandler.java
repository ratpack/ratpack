package org.ratpackframework.handler;

/**
 * Convenience subtype for a handler that takes a result.
 *
 * @param <T> The type of the successful result object.
 */
public interface ResultHandler<T> extends Handler<Result<T>> {

}
