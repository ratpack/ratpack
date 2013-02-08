package org.ratpackframework.config;

public class DeploymentConfig {

  int port = 5050;
  String bindHost = null;
  String publicHost = null;

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getBindHost() {
    return bindHost;
  }

  public void setBindHost(String bindHost) {
    this.bindHost = bindHost;
  }

  public String getPublicHost() {
    return publicHost;
  }

  public void setPublicHost(String publicHost) {
    this.publicHost = publicHost;
  }
}
