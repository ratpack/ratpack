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

import ratpack.file.FileSystemBinding;
import ratpack.file.checksummer.internal.Adler32Checksummer;
import ratpack.file.checksummer.internal.MD5Checksummer;
import ratpack.func.Function;
import ratpack.server.ServerConfig;

import java.io.InputStream;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.nio.file.Files;

/**
 *  Factory methods for creating {@link FileSystemChecksumService}.
 *  <p>
 *  Checksum service is used for calculation of file system checksums - assets or any kind of files.
 *  Checksum service is backed by either predefined checksum calculation function (Noop. Adler32, MD5), or custom function.
 *  Custom function has to be provided in the form of {@code Function<InputStream, String>} (class implementing this interface or lambda expression).
 *  If checksummer function is not provided then Noop (no operation) calculation method is used. It does nothing, returns empty string as checksum.
 *
 *
 * <pre>{@code
 * import ratpack.file.internal.FileSystemChecksumService;
 * import ratpack.file.internal.FileSystemChecksumServices;
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.test.handling.RequestFixture;
 * import ratpack.test.handling.HandlingResult;
 * import static org.junit.Assert.assertNotNull;
 * import java.nio.file.Paths;
 *
 * public class Test {
 *   public static class MyHandler implements Handler {
 *     public void handle(Context ctx) throws Exception {
 *       FileSystemChecksumService service = FileSystemChecksumServices.adler32(ctx.getServerConfig(), "assets", "css", "js", "png", "svg");
 *       try {
 *         String chksum = service.checksum("styles/ratpack.css");
 *         ctx.render(chksum);
 *       }
 *       catch (Exception ex) {
 *         ctx.clientError(400);
 *       }
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     // Paths.get(".") -> indicates ratpack-manual home folder.
 *     HandlingResult result = RequestFixture.requestFixture()
 *       .serverConfig(Paths.get("../ratpack-site/src/ratpack"), b -> {
 *       })
 *       .handle(new MyHandler());
 *     assertNotNull(result.rendered(String.class));
 *   }
 * }
 * }</pre>
 */
public abstract class FileSystemChecksumServices {

  private FileSystemChecksumServices() {
  }

  /**
   *  Get checksum service that is backward compatible - calculates file checksum with Adler32 method.
   *
   *  @param serverConfig current server configuration. The most important parameter is baseDir. File path taken as parameter to checksummer is calculated relative to baseDir.
   *  @return file system checksummer service
   */
  public static FileSystemChecksumService service(ServerConfig serverConfig) {
    return service(serverConfig, new Adler32Checksummer());
  }

  /**
   *  Get checksum service with calculation method given as checksummer function.
   *  If checksummerFunc is not provided then noop method (no calculation) is used.
   *
   *  @param serverConfig current server configuration. The most important parameter is baseDir. File path taken as parameter to checksummer is calculated relative to baseDir.
   *  @param checksummerFunc checksum calculation function that takes InputStream and return String with checksum value.
   *  @return file system checksummer service
   */
  public static FileSystemChecksumService service(ServerConfig serverConfig, Function<? super InputStream, ? extends String> checksummerFunc) {
    return service(serverConfig, checksummerFunc, null);
  }

  /**
   *  Get checksum service for additional path related to server's base dir and calculation method as checksummer function.
   *  Apply file filtering by extension: <i>js</i>, <i>css</i>, <i>png</i> filter files in target path.
   *  If checksummer function is not provided then noop method is used (no checksum calculation).
   *  If additional path is not given then server's base dir is used.
   *  If additional path is not correct path definition (contains illegal characters) or is not existing directory, IllegalArgumentException is thrown.
   *  If fileEndsWith variable length argument is not given then checksum may be calculated for file with any extension.
   *  If fileEndsWith is given and file's extension no match NoSuchFileException is thrown.
   *
   *  @param serverConfig server configuration. The most important parameter is baseDir. File path taken as parameter to checksummer is calculated relative to baseDir and additional path.
   *  @param checksummerFunc checksum calculation function
   *  @param path additional path calculated relative to server's base dir. Becomes root for checksummer function.
   *  @param fileEndsWith variable length array of extenstions filtering files in target path
   *  @return file system checksummer service
   */
  public static FileSystemChecksumService service(ServerConfig serverConfig, Function<? super InputStream, ? extends String> checksummerFunc, String path, String... fileEndsWith) {
    Function<? super InputStream, ? extends String> checksummer = checksummerFunc != null ? checksummerFunc : noopChecksummer();
    FileSystemBinding fsb = path != null ? serverConfig.getBaseDir().binding(path) : serverConfig.getBaseDir();
    List<String> exts = Arrays.asList(fileEndsWith);
    if (fsb == null || !Files.isDirectory(fsb.getFile())) {
      throw new IllegalArgumentException("Non existing path related to server's base dir.");
    }
    DefaultFileSystemChecksumService service = new DefaultFileSystemChecksumService(fsb, checksummer, exts);
    if (serverConfig.isDevelopment()) {
      return service;
    } else {
      CachingFileSystemChecksumService cachingService = new CachingFileSystemChecksumService(service);
      new FileSystemChecksumServicePopulater(fsb.getFile(), exts, cachingService, Executors.newFixedThreadPool(5), 4).start();
      return cachingService;
    }
  }

  /**
   *  Get checksum service with Adler32 calculation method.
   *
   *  @param serverConfig current server configuration. The most important parameter is baseDir. File path taken as parameter to checksummer is calculated relative to baseDir.
   *  @param path additional path related to server's base dir. Becomes root for checksummer function. Optional argument.
   *  @param fileEndsWith variable length array of extenstions filtering files in target path
   *  @return file system checksummer service
   */
  public static FileSystemChecksumService adler32(ServerConfig serverConfig, String path, String... fileEndsWith) {
    return service(serverConfig, new Adler32Checksummer(), path, fileEndsWith);
  }

  /**
   *  Get checksum service with MD5 calculation method.
   *
   *  @param serverConfig current server configuration. The most important parameter is baseDir. File path taken as parameter to checksummer is calculated relative to baseDir.
   *  @param path additional path related to server's base dir. Becomes root for checksummer function. Optional argument.
   *  @param fileEndsWith variable length array of extenstions filtering files in target path
   *  @return file system checksummer service
   */
  public static FileSystemChecksumService md5(ServerConfig serverConfig, String path, String... fileEndsWith) {
    return service(serverConfig, new MD5Checksummer(), path, fileEndsWith);
  }

  /**
   *  Return checksummer that does nothing. Does not calculate checksum and instead returns empty string.
   *  @return checksummer function that returns empty checksum.
   */
  private static Function<InputStream, String> noopChecksummer() {
    return (InputStream is) -> {
      return "";
    };
  }
}
