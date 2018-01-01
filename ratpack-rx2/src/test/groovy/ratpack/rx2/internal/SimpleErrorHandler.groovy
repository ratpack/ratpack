package ratpack.rx2.internal

import ratpack.error.ServerErrorHandler
import ratpack.handling.Context

class SimpleErrorHandler implements ServerErrorHandler {

  @Override
  public void error(Context context, Throwable throwable) throws Exception {
    throwable.printStackTrace()
    context.response.status(500).send(throwable.toString())
  }
}
