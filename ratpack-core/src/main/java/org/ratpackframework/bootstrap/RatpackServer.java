package org.ratpackframework.bootstrap;

import com.google.common.util.concurrent.Service;

/**
 * A ratpack server.
 */
public interface RatpackServer extends Service {

  /**
   * The actual port that the application is bound to.
   */
  int getBindPort();

  /**
   * The actual host/ip that the application is bound to.
   */
  String getBindHost();

}
