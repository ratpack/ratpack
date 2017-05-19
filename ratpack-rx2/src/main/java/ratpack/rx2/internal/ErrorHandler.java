package ratpack.rx2.internal;

import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Consumer;
import ratpack.exec.Promise;
import ratpack.func.Action;

public class ErrorHandler implements Consumer<Throwable> {
  @Override
  public void accept(Throwable e) throws Exception {
    if (e instanceof OnErrorNotImplementedException) {
      Promise.error(e.getCause()).then(Action.noop());
    } else if (e instanceof UndeliverableException) {
      Promise.error(e.getCause()).then(Action.noop());
    } else {
      Promise.error(e).then(Action.noop());
    }
  }
}
