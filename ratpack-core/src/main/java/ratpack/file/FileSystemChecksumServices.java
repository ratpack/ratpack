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

package ratpack.file;

import ratpack.file.internal.DefaultFileSystemChecksumService;
import ratpack.file.internal.FileSystemChecksumServicePopulater;
import ratpack.func.Function;
import ratpack.launch.ServerConfig;

import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public abstract class FileSystemChecksumServices {

  private FileSystemChecksumServices() {
  }

  public static FileSystemChecksumService service(ServerConfig serverConfig) {
    Function<InputStream, String> checksummer = new Adler32Checksummer();
    DefaultFileSystemChecksumService service = new DefaultFileSystemChecksumService(serverConfig.getBaseDir(), checksummer);
    if (serverConfig.isDevelopment()) {
      return service;
    } else {
      CachingFileSystemChecksumService cachingService = new CachingFileSystemChecksumService(service);
      new FileSystemChecksumServicePopulater(serverConfig.getBaseDir().getFile(), cachingService, Executors.newFixedThreadPool(5), 4).start();
      return cachingService;
    }
  }

  private static class Adler32Checksummer implements Function<InputStream, String> {
    private static final int BUFFER_SIZE = 8192;

    @Override
    public String apply(InputStream inputStream) throws Exception {
      byte[] buffer = new byte[BUFFER_SIZE];
      Checksum checksum = new Adler32();
      int read = inputStream.read(buffer, 0, BUFFER_SIZE);
      while (read != -1) {
        checksum.update(buffer, 0, read);
        read = inputStream.read(buffer, 0, BUFFER_SIZE);
      }

      return Long.toHexString(checksum.getValue());
    }
  }

}
