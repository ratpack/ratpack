package org.ratpackframework.file;

import java.io.File;

public interface FileSystemContext {

  File getFile();

  File file(String... path);

  FileSystemContext context(String... path);

  FileSystemContext context(File file);

}
