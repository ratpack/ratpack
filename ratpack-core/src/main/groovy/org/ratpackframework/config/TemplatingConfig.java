package org.ratpackframework.config;

public class TemplatingConfig {

  private String dir = "templates";
  private int cacheSize = 0;
  private boolean staticallyCompile;

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public boolean isStaticallyCompile() {
    return staticallyCompile;
  }

  public void setStaticallyCompile(boolean staticallyCompile) {
    this.staticallyCompile = staticallyCompile;
  }
}
