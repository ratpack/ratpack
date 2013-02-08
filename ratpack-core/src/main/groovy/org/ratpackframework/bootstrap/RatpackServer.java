package org.ratpackframework.bootstrap;

import com.google.common.util.concurrent.Service;

public interface RatpackServer extends Service {

  int getBindPort();

  String getBindHost();
}
