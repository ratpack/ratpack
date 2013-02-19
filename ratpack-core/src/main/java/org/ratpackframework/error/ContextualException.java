package org.ratpackframework.error;

/**
 * A marker interface to indicate that if an exception has a cause, it is merely providing context.
 *
 * That is, it should not be considered the root cause.
 */
public interface ContextualException {}
