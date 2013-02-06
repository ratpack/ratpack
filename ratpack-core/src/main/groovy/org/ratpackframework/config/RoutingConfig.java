package org.ratpackframework.config;

public class RoutingConfig {

  private String file = "ratpack.groovy";
  private boolean staticallyCompile;
  private boolean reloadable = true;

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public boolean isStaticallyCompile() {
    return staticallyCompile;
  }

  public void setStaticallyCompile(boolean staticallyCompile) {
    this.staticallyCompile = staticallyCompile;
  }

  public boolean isReloadable() {
    return reloadable;
  }

  public void setReloadable(boolean reloadable) {
    this.reloadable = reloadable;
  }
}
