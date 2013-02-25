package org.ratpackframework.handler;

/**
 * The result of an asynchronous operation, which may be a failure.
 *
 * @param <T> The type of the successful result object.
 */
public class Result<T> {

  private final Exception failure;
  private final T value;

  public Result(Exception failure) {
    this.failure = failure;
    this.value = null;
  }

  public <Y extends T> Result(Y value) {
    this.value = value;
    this.failure = null;
  }

  public Exception getFailure() {
    return failure;
  }

  public T getValue() {
    return value;
  }

  public boolean isSuccess() {
    return failure == null;
  }

  public boolean isFailure() {
    return failure != null;
  }

}
