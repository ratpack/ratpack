package org.ratpackframework.session.store;

import java.util.concurrent.ConcurrentMap;

public interface SessionStorage extends ConcurrentMap<String, Object> {
}
