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

package ratpack.file;

import ratpack.handling.Chain;
import ratpack.path.PathBinding;

/**
 * A specification for a handler that serves files from the file system.
 *
 * @see Chain#files(ratpack.func.Action)
 */
public interface FileHandlerSpec {

  /**
   * Specifies the request path to bind to for serving files.
   * <p>
   * The path specified is relative to the context's {@link PathBinding} at request time.
   * The portion of the request path <i>past</i> the path binding identifies the target file to serve.
   * The default value is effectively {@code ""}, which means that existing path binding is used.
   *
   * <pre class="java">{@code
   * import ratpack.test.embed.EphemeralBaseDir;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EphemeralBaseDir.tmpDir().use(baseDir -> {
   *       baseDir.write("a.txt", "a");
   *       EmbeddedApp.of(s -> s
   *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
   *         .handlers(c -> c
   *           .files(f -> f.path("files"))
   *           .prefix("prefix", p -> p
   *               .files()
   *               .files(f -> f.path("files"))
   *           )
   *         )
   *       ).test(httpClient -> {
   * //        assertEquals("a", httpClient.getText("files/a.txt"));
   * //        assertEquals("a", httpClient.getText("prefix/a.txt"));
   *         assertEquals("a", httpClient.getText("prefix/files/a.txt"));
   *       });
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param path the request path to bind to
   * @return {@code this}
   */
  FileHandlerSpec path(String path);

  /**
   * Specifies the file system path to the files.
   * <p>
   * The path specified is relative to the context's {@link FileSystemBinding} at request time.
   * The default value is effectively {@code ""}, which means that existing file system binding is used.
   *
   * <pre class="java">{@code
   * import ratpack.test.embed.EphemeralBaseDir;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EphemeralBaseDir.tmpDir().use(baseDir -> {
   *       baseDir.write("dir/a.txt", "a");
   *       EmbeddedApp.of(s -> s
   *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
   *         .handlers(c -> c
   *           .files(f -> f.path("nopath"))
   *           .files(f -> f.path("pathed").dir("dir"))
   *           .prefix("under-binding", p -> p
   *             .fileSystem("dir", f -> f
   *                 .files()
   *             )
   *           )
   *         )
   *       ).test(httpClient -> {
   *         assertEquals("a", httpClient.getText("nopath/dir/a.txt"));
   *         assertEquals("a", httpClient.getText("pathed/a.txt"));
   *         assertEquals("a", httpClient.getText("under-binding/a.txt"));
   *       });
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param dir the file system path to bind to
   * @return {@code this}
   */
  FileHandlerSpec dir(String dir);

  /**
   * A convenience method that specifies both request path and file system path bindings for serving files.
   * <p>
   * The path specified is relative to the context's {@link PathBinding} and {@link FileSystemBinding} at request time.
   * The default value is effectively {@code ""}, which means that existing path binding and file system binding is used.
   *
   * <pre class="java">{@code
   * import ratpack.test.embed.EphemeralBaseDir;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EphemeralBaseDir.tmpDir().use(baseDir -> {
   *       baseDir.write("dir/a.txt", "a");
   *       EmbeddedApp.of(s -> s
   *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
   *         .handlers(c -> c
   *           .files(f ->
   *               f.files("dir") // same as f.path("dir").dir("dir")
   *           )
   *         )
   *       ).test(httpClient ->
   *         assertEquals("a", httpClient.getText("/dir/a.txt"))
   *       );
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param path the request path and file system path to bind to
   * @return {@code this}
   */
  default FileHandlerSpec files(String path) {
    return path(path).dir(path);
  }

  /**
   * The files that should be used when a request is made for a directory.
   * <p>
   * If the request path matches a directory, an index file may be served.
   * The directory is checked for presence of the given index files.
   * The first that is found is served.
   *
   * <pre class="java">{@code
   * import ratpack.test.embed.EphemeralBaseDir;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EphemeralBaseDir.tmpDir().use(baseDir -> {
   *       baseDir.write("dir1/a.txt", "a1");
   *       baseDir.write("dir1/b.txt", "b1");
   *       baseDir.write("dir2/b.txt", "b2");
   *       EmbeddedApp.of(s -> s
   *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
   *         .handlers(c -> c
   *           .files(f -> f.path("noIndex"))
   *           .files(f -> f.path("indexes").indexFiles("a.txt", "b.txt"))
   *         )
   *       ).test(httpClient -> {
   *         httpClient.requestSpec(r -> r.redirects(1));
   *         assertEquals(404, httpClient.get("noIndex/dir1").getStatusCode());
   *         assertEquals("a1", httpClient.getText("indexes/dir1"));
   *         assertEquals("b2", httpClient.getText("indexes/dir2"));
   *       });
   *     });
   *   }
   * }
   * }</pre>
   *
   * @param indexFiles the index files in order of precedence
   * @return {@code this}
   */
  FileHandlerSpec indexFiles(String... indexFiles);

}
