package org.ratpackframework.assets;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The configuration of how to serve static assets.
 */
public class StaticAssetsConfig {

  private File directory;
  private List<String> indexFiles = new LinkedList<>(Arrays.asList("index.html"));

  public StaticAssetsConfig(File directory) {
    this.directory = directory;
  }

  /**
   * The directory that contains the static assets to serve.
   */
  public File getDirectory() {
    return directory;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
  }

  /**
   * The name of files that can be served when the request is for a directory.
   *
   * Defaults to just "index.html".
   */
  public List<String> getIndexFiles() {
    return indexFiles;
  }

  public void setIndexFiles(List<String> indexFiles) {
    this.indexFiles = indexFiles;
  }

}
