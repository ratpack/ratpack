package org.ratpackframework.file.internal;

import org.ratpackframework.file.FileSystemContext;

import java.io.File;

import static org.ratpackframework.util.CollectionUtils.join;

public class DefaultFileSystemContext implements FileSystemContext {

  private final File file;

  public DefaultFileSystemContext(File file) {
    this.file = file;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public File file(String... path) {
    return new File(file, join(File.separator, (Object[]) path));
  }

  @Override
  public FileSystemContext context(String... path) {
    return new DefaultFileSystemContext(file(path));
  }

  @Override
  public FileSystemContext context(File file) {
    return new DefaultFileSystemContext(file);
  }
}
