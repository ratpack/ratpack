/*
 * Copyright 2014 the original author or authors.
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

package ratpack.file.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

public class FileSystemChecksumServicePopulater {

  private final FileSystemChecksumService checksumService;
  private final ExecutorService executorService;
  private final int workers;
  private final Path root;
  private final List<String> fileEndsWith;

  private static class Task {
    private final String path;

    private Task(String path) {
      this.path = path;
    }
  }

  private final LinkedBlockingQueue<Task> queue = new LinkedBlockingQueue<>();

  private AtomicBoolean started = new AtomicBoolean();
  private final CountDownLatch latch;

  public FileSystemChecksumServicePopulater(Path root, List<String> fileEndsWith, FileSystemChecksumService checksumService, ExecutorService executorService, int workers) {
    this.root = root;
    this.fileEndsWith = fileEndsWith;
    this.checksumService = checksumService;
    this.executorService = executorService;
    this.workers = workers;
    this.latch = new CountDownLatch(this.workers);
  }

  public boolean start() {
    if (!started.compareAndSet(false, true)) {
      return false;
    }

    executorService.submit(new FileSystemWalk());
    for (int i = 0; i < workers; ++i) {
      executorService.submit(new Checksummer());
    }

    return true;
  }

  private class FileSystemWalk implements Runnable {
    @Override
    public void run() {
      try {
        Files.walkFileTree(root, new FileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
              String nPath = root.relativize(file).toString();
              if (fileEndsWith != null && !fileEndsWith.isEmpty()) {
                if (fileEndsWith.stream().noneMatch(nPath::endsWith)) {
                  return FileVisitResult.CONTINUE;
                }
              }
              queue.put(new Task(nPath));
              return FileVisitResult.CONTINUE;
            } catch (InterruptedException e) {
              return FileVisitResult.TERMINATE;
            }
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException ignore) {
        // ignore
      } finally {
        try {
          queue.put(new Task(null));
        } catch (InterruptedException ignore) {
          // ignore
        }
      }
    }
  }

  private class Checksummer implements Runnable {
    @Override
    public void run() {
      try {
        Task entry = queue.take();
        while (entry.path != null) {
          try {
            checksumService.checksum(entry.path);
          } catch (Exception ignore) {
            // ignore
          }
          entry = queue.take();
        }
        queue.put(entry);
      } catch (InterruptedException ignore) {
        // ignore
      } finally {
        latch.countDown();
      }
    }
  }

  public void waitFor() throws InterruptedException {
    start();
    latch.await();
  }

}
