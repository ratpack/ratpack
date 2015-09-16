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

package ratpack.test.embed.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static ratpack.util.Exceptions.uncheck;

public class JarFileEphemeralBaseDir extends PathEphemeralBaseDir {

  public JarFileEphemeralBaseDir(File jar) {
    super(getJarPath(jar), jar.toPath());
  }

  @Override
  public Path write(String path, String content) {
    Path file = super.write(path, content);
    FileSystem fileSystem = file.getFileSystem();

    // We have to force the write to disk.
    // ZipFileSystem doesn't respect the SYNC flag.
    try {
      call("beginWrite", fileSystem);
      call("sync", fileSystem);
      call("endWrite", fileSystem);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return file;
  }

  private void call(String methodName, FileSystem fileSystem) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = fileSystem.getClass().getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(fileSystem);
  }

  private static Path getJarPath(File jar) {
    URI uri = URI.create("jar:" + jar.toURI().toString());
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    FileSystem fileSystem;
    try {
      fileSystem = FileSystems.newFileSystem(uri, env);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return fileSystem.getPath("/");
  }

}
