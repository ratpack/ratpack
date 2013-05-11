package org.ratpackframework.session;

public class SessionCookieConfig {

  private int expiresMins = 60 * 60 * 24 * 365; // 1 year

  private String domain;

  private String path = "/";

  public int getExpiresMins() {
    return expiresMins;
  }

  public void setExpiresMins(int expiresMins) {
    this.expiresMins = expiresMins;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

}
