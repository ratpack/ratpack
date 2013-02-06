package org.ratpackframework.bootstrap.internal;

import org.ratpackframework.bootstrap.RatpackApp;
import org.vertx.java.core.Handler;

public class NoopInit implements Handler<RatpackApp> {

  @Override
  public void handle(RatpackApp event) {
  }

}
