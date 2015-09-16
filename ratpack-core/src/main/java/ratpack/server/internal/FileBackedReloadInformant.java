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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import ratpack.registry.Registry;
import ratpack.server.ReloadInformant;
import ratpack.util.internal.IoUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static ratpack.util.Exceptions.uncheck;

public class FileBackedReloadInformant implements ReloadInformant {
  private final Path file;
  private final Lock lock = new ReentrantLock();
  private final AtomicReference<FileTime> lastModifiedHolder = new AtomicReference<>(null);
  private final AtomicReference<ByteBuf> contentHolder = new AtomicReference<>();

  public FileBackedReloadInformant(Path file) {
    this.file = file;
    load();
  }

  private void load() {
    lock.lock();
    try {
      FileTime lastModifiedTime = Files.getLastModifiedTime(file);
      ByteBuf bytes = IoUtils.read(UnpooledByteBufAllocator.DEFAULT, file);

      this.lastModifiedHolder.set(lastModifiedTime);
      this.contentHolder.set(bytes);
    } catch (Exception e) {
      throw uncheck(e);
    } finally {
      lock.unlock();
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
      return reloadNeeded();
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  @Override
  public String toString() {
    return "file-backed reload informant: " + file.toString();
  }

  private boolean reloadNeeded() throws IOException {
    return !isBytesAreSame();
  }

  private boolean isBytesAreSame() throws IOException {
    lock.lock();
    try {
      ByteBuf existing = contentHolder.get();
      //noinspection SimplifiableIfStatement
      if (existing == null) {
        return false;
      }

      return IoUtils.read(UnpooledByteBufAllocator.DEFAULT, file).equals(existing);
    } finally {
      lock.unlock();
    }
  }
}
