/*
 * Copyright 2013 the original author or authors.
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

package ratpack.test.embed;

import ratpack.func.Action;
import ratpack.test.embed.internal.JarFileEphemeralBaseDir;
import ratpack.test.embed.internal.PathEphemeralBaseDir;
import ratpack.util.Exceptions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;

/**
 * A helper for creating a base dir programmatically, typically at test time.
 * <p>
 * This is typically used in conjunction with {@link EmbeddedApp}, when the app requires a base dir.
 * It is however also useful for any kind of testing where a “filesystem” is needed just for the test.
 * <p>
 * The {@link #tmpDir()}, {@link #dir(File)}, {@link #tmpJar()} and {@link #jar(File)} methods create instances,
 * controlling what file system space will be used.
 * <p>
 * The {@link #write(String, String)}, {@link #mkdir(String)} and {@link #path(String)} methods can be used for creating and getting at content.
 * <p>
 * The {@link #getRoot()} method provides a path object for the root of the base dir.
 * <p>
 * This type implements {@link Closeable}.
 * Closing an embedded base dir causes the base dir to be <b>removed</b> from the real file system.
 * It is generally always desirable to close the base dir in order not to leave files around.
 * The {@link #use(Action)} method allows an action to be executed before having the base dir be automatically closed.
 */
public interface EphemeralBaseDir extends AutoCloseable {

  /**
   * Creates a new base dir, using a newly created dir within the JVM's assigned temp dir.
   *
   * @return a new embedded base dir
   */
  static EphemeralBaseDir tmpDir() {
    return dir(com.google.common.io.Files.createTempDir());
  }

  /**
   * Creates a new base dir, using the given dir as the root.
   * <p>
   * <b>Note:</b> if the returned base dir is closed, the given dir will be deleted.
   *
   * @param dir the base dir root
   * @return a new embedded base dir
   */
  static EphemeralBaseDir dir(File dir) {
    return new PathEphemeralBaseDir(dir);
  }

  /**
   * Creates a new base dir which is actually a jar created within the JVM's assigned temp dir.
   * <p>
   * This is typically used when testing Ratpack extensions to verify that they don't assume they are
   * running from the default file system.
   *
   * @return a new embedded base dir
   */
  static EphemeralBaseDir tmpJar() {
    return jar(Exceptions.uncheck(() -> File.createTempFile("ratpack", ".jar")));
  }

  /**
   * Creates a new base dir which is actually a jar at the given location.
   * <p>
   * This is typically used when testing Ratpack extensions to verify that they don't assume they are
   * running from the default file system.
   * <p>
   * The given file is expected to exist and be empty.
   *
   * @param jarFile the location of the jar to act as a base dir
   * @return a new embedded base dir
   */
  static EphemeralBaseDir jar(File jarFile) {
    return new JarFileEphemeralBaseDir(jarFile);
  }

  /**
   * Add's a JVM shutdown hook that will {@link #close()} this base dir.
   *
   * @return {@code this}
   */
  default EphemeralBaseDir closeOnExit() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          close();
        } catch (IOException e) {
          throw Exceptions.uncheck(e);
        }
      }
    });
    return this;
  }

  /**
   * Executes the given action with this base dir, then closes this base dir.
   *
   * @param action the action to execute before closing this base dir
   * @throws Exception any thrown by {@code action}
   */
  default void use(Action<? super EphemeralBaseDir> action) throws Exception {
    try {
      action.execute(this);
    } finally {
      close();
    }
  }

  /**
   * Returns a path for the given path within the base dir.
   * <p>
   * All parent directories will be created on demand.
   *
   * @param path the relative path to the path
   * @return a path for the given path
   */
  default Path path(String path) {
    Path pathObj = getRoot().resolve(path);
    Exceptions.uncheck(() -> Files.createDirectories(pathObj.getParent()));
    return pathObj;
  }

  /**
   * Creates a file with the given string content at the given path within the base dir.
   * <p>
   * All parent directories will be created on demand.
   *
   * @param path the relative path to the file to create
   * @param content the content to write to the file
   * @return a path for the created file
   */
  default Path write(String path, String content) {
    Path file = path(path);
    Exceptions.uncheck(() -> {
      Files.write(file, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
    });
    return file;
  }

  /**
   * Creates a directory at the given path within the base dir.
   * <p>
   * All parent directories will be created on demand.
   *
   * @param path the relative path to the file to create
   * @return a path for the created directory
   */
  default Path mkdir(String path) {
    return Exceptions.uncheck(() -> Files.createDirectories(getRoot().resolve(path)));
  }

  /**
   * The root of the base dir.
   *
   * @return the root of the base dir
   */
  Path getRoot();

  /**
   * Deletes the base dir from the file system.
   *
   * @throws IOException if the base dir cannot be deleted.
   */
  @Override
  void close() throws IOException;
}
