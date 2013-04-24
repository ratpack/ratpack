package org.ratpackframework.bootstrap.internal;

import org.ratpackframework.bootstrap.RatpackServer;
import org.ratpackframework.Action;

public class NoopInit implements Action<RatpackServer> {

  @Override
  public void execute(RatpackServer event) {
  }

}
