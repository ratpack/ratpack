package org.ratpackframework.config;

public class SessionCookieConfig {

  private int expiresMins = 60 * 60 * 24 * 365; // 1 year

  public int getExpiresMins() {
    return expiresMins;
  }

  public void setExpiresMins(int expiresMins) {
    this.expiresMins = expiresMins;
  }
}
