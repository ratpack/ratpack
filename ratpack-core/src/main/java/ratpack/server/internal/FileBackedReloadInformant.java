/*
 * Copyright 2015 the original author or authors.
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

package ratpack.server.internal;

import ratpack.registry.Registry;
import ratpack.server.ReloadInformant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicReference;

import static ratpack.util.Exceptions.uncheck;

public class FileBackedReloadInformant implements ReloadInformant {
  private final Path file;
  private final AtomicReference<FileTime> lastModifiedHolder = new AtomicReference<>(null);

  public FileBackedReloadInformant(Path file) {
    this.file = file;
    probe();
  }

  private boolean probe() {
    try {
      FileTime nowLastModified = Files.getLastModifiedTime(file);
      FileTime previousLastModified = lastModifiedHolder.getAndSet(nowLastModified);
      return !nowLastModified.equals(previousLastModified);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  @Override
  public boolean shouldReload(Registry registry) {
    // If the file disappeared, wait a little for it to appear
    int i = 10;
    while (!Files.exists(file) && --i > 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw uncheck(e);
      }
    }

    if (!Files.exists(file)) {
      return false;
    }

    try {
      return probe();
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  @Override
  public String toString() {
    return "file-backed reload informant: " + file.toString();
  }

}
