package org.ratpackframework.bootstrap.internal;

import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.handler.Handler;

public class NoopInit implements Handler<RatpackServer> {

  @Override
  public void handle(RatpackServer event) {
  }

}
