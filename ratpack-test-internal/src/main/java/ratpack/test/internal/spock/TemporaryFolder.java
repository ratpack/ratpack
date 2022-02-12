/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.test.internal.spock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TemporaryFolder {

  private File root;

  public TemporaryFolder(File root) {
    this.root = root;
  }

  public File getRoot() {
    return root;
  }

  // Below here is taken from https://github.com/junit-team/junit4/blob/main/src/main/java/org/junit/rules/TemporaryFolder.java
  // For backwards compatability

  private static final int TEMP_DIR_ATTEMPTS = 10000;
  private static final String TMP_PREFIX = "junit";

  /**
   * Returns a new fresh file with the given name under the temporary folder.
   */
  public File newFile(String fileName) throws IOException {
    File file = new File(getRoot(), fileName);
    if (!file.createNewFile()) {
      throw new IOException(
        "a file with the name \'" + fileName + "\' already exists in the test folder");
    }
    return file;
  }

  /**
   * Returns a new fresh file with a random name under the temporary folder.
   */
  public File newFile() throws IOException {
    return File.createTempFile(TMP_PREFIX, null, getRoot());
  }

  /**
   * Returns a new fresh folder with the given path under the temporary
   * folder.
   */
  public File newFolder(String path) throws IOException {
    return newFolder(new String[]{path});
  }

  /**
   * Returns a new fresh folder with the given paths under the temporary
   * folder. For example, if you pass in the strings {@code "parent"} and {@code "child"}
   * then a directory named {@code "parent"} will be created under the temporary folder
   * and a directory named {@code "child"} will be created under the newly-created
   * {@code "parent"} directory.
   */
  public File newFolder(String... paths) throws IOException {
    if (paths.length == 0) {
      throw new IllegalArgumentException("must pass at least one path");
    }

    /*
     * Before checking if the paths are absolute paths, check if create() was ever called,
     * and if it wasn't, throw IllegalStateException.
     */
    File root = getRoot();
    for (String path : paths) {
      if (new File(path).isAbsolute()) {
        throw new IOException("folder path \'" + path + "\' is not a relative path");
      }
    }

    File relativePath = null;
    File file = root;
    boolean lastMkdirsCallSuccessful = true;
    for (String path : paths) {
      relativePath = new File(relativePath, path);
      file = new File(root, relativePath.getPath());

      lastMkdirsCallSuccessful = file.mkdirs();
      if (!lastMkdirsCallSuccessful && !file.isDirectory()) {
        if (file.exists()) {
          throw new IOException(
            "a file with the path \'" + relativePath.getPath() + "\' exists");
        } else {
          throw new IOException(
            "could not create a folder with the path \'" + relativePath.getPath() + "\'");
        }
      }
    }
    if (!lastMkdirsCallSuccessful) {
      throw new IOException(
        "a folder with the path \'" + relativePath.getPath() + "\' already exists");
    }
    return file;
  }

  /**
   * Returns a new fresh folder with a random name under the temporary folder.
   */
  public File newFolder() throws IOException {
    return createTemporaryFolderIn(getRoot());
  }

  private static File createTemporaryFolderIn(File parentFolder) throws IOException {
    try {
      return createTemporaryFolderWithNioApi(parentFolder);
    } catch (ClassNotFoundException ignore) {
      // Fallback for Java 5 and 6
      return createTemporaryFolderWithFileApi(parentFolder);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException) cause;
      }
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      IOException exception = new IOException("Failed to create temporary folder in " + parentFolder);
      exception.initCause(cause);
      throw exception;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create temporary folder in " + parentFolder, e);
    }
  }

  private static File createTemporaryFolderWithNioApi(File parentFolder) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> filesClass = Class.forName("java.nio.file.Files");
    Object fileAttributeArray = Array.newInstance(Class.forName("java.nio.file.attribute.FileAttribute"), 0);
    Class<?> pathClass = Class.forName("java.nio.file.Path");
    Object tempDir;
    if (parentFolder != null) {
      Method createTempDirectoryMethod = filesClass.getDeclaredMethod("createTempDirectory", pathClass, String.class, fileAttributeArray.getClass());
      Object parentPath = File.class.getDeclaredMethod("toPath").invoke(parentFolder);
      tempDir = createTempDirectoryMethod.invoke(null, parentPath, TMP_PREFIX, fileAttributeArray);
    } else {
      Method createTempDirectoryMethod = filesClass.getDeclaredMethod("createTempDirectory", String.class, fileAttributeArray.getClass());
      tempDir = createTempDirectoryMethod.invoke(null, TMP_PREFIX, fileAttributeArray);
    }
    return (File) pathClass.getDeclaredMethod("toFile").invoke(tempDir);
  }

  private static File createTemporaryFolderWithFileApi(File parentFolder) throws IOException {
    File createdFolder = null;
    for (int i = 0; i < TEMP_DIR_ATTEMPTS; ++i) {
      // Use createTempFile to get a suitable folder name.
      String suffix = ".tmp";
      File tmpFile = File.createTempFile(TMP_PREFIX, suffix, parentFolder);
      String tmpName = tmpFile.toString();
      // Discard .tmp suffix of tmpName.
      String folderName = tmpName.substring(0, tmpName.length() - suffix.length());
      createdFolder = new File(folderName);
      if (createdFolder.mkdir()) {
        tmpFile.delete();
        return createdFolder;
      }
      tmpFile.delete();
    }
    throw new IOException("Unable to create temporary directory in: "
      + parentFolder.toString() + ". Tried " + TEMP_DIR_ATTEMPTS + " times. "
      + "Last attempted to create: " + createdFolder.toString());
  }
}
