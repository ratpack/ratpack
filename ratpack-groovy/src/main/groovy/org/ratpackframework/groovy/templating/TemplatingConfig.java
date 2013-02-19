package org.ratpackframework.groovy.templating;

import java.io.File;

public class TemplatingConfig {

  private File directory;
  private int cacheSize = 0;
  private boolean staticallyCompile;

  public TemplatingConfig(File directory) {
    this.directory = directory;
  }

  public File getDirectory() {
    return directory;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
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
