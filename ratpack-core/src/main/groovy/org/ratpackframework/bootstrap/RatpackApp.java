package org.ratpackframework.bootstrap;

public interface RatpackApp {
  void start();

  void startAndWait();

  void stop();

  boolean isRunning();

  int getBindPort();

  String getBindHost();
}
