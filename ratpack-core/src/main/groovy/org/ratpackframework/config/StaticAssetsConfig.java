package org.ratpackframework.config;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class StaticAssetsConfig {

  private String dir = "public";
  private List<String> indexFiles = new LinkedList<>(Arrays.asList("index.html"));

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public List<String> getIndexFiles() {
    return indexFiles;
  }

  public void setIndexFiles(List<String> indexFiles) {
    this.indexFiles = indexFiles;
  }
}
