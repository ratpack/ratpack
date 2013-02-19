package org.ratpackframework.assets;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class StaticAssetsConfig {

  private File directory;
  private List<String> indexFiles = new LinkedList<>(Arrays.asList("index.html"));

  public StaticAssetsConfig() {
    this(new File(System.getProperty("user.dir")));
  }

  public StaticAssetsConfig(File directory) {
    this.directory = directory;
  }

  public File getDirectory() {
    return directory;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
  }

  public List<String> getIndexFiles() {
    return indexFiles;
  }

  public void setIndexFiles(List<String> indexFiles) {
    this.indexFiles = indexFiles;
  }

}
