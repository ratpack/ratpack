package org.ratpackframework.config;

import java.io.File;

public class LayoutConfig {

  private File baseDir;

  public LayoutConfig(File baseDir) {
    this.baseDir = baseDir;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

}
