package org.ratpackframework.session.store;

import org.ratpackframework.http.Request;
import org.ratpackframework.session.Session;

import java.util.concurrent.ConcurrentMap;

public interface MapSessionStore {
  SessionStorage get(String sessionId);
  long size();
}
