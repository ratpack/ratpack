package org.ratpackframework.templating.internal;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.buffer.Buffer;

public class NestedTemplate implements AsyncResultHandler<ExecutedTemplate> {

  private AsyncResult<ExecutedTemplate> event;

  @Override
  public void handle(AsyncResult<ExecutedTemplate> result) {
    this.event = result;
  }

  public AsyncResult<ExecutedTemplate> getEvent() {
    return event;
  }

  void render(Buffer buffer) {
    if (event.succeeded()) {
      event.result.render(buffer);
    }
  }
}
