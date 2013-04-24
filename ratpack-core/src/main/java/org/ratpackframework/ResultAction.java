package org.ratpackframework;

/**
 * Convenience subtype for an action that takes a result.
 *
 * @param <T> The type of the successful result object.
 */
public interface ResultAction<T> extends Action<Result<T>> {

}
