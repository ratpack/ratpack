package org.ratpackframework.session.internal;

import org.ratpackframework.session.SessionListener;

public class NoopSessionListener implements SessionListener {
  @Override
  public void sessionInitiated(String id) {
  }

  @Override
  public void sessionTerminated(String id) {
  }
}
