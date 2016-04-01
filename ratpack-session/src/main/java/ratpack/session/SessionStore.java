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

package ratpack.session;

import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import ratpack.exec.Operation;
import ratpack.exec.Promise;

/**
 * A persistent store of session data.
 * <p>
 * Ratpack's session support cooperates with the implementation of this type found in the context registry.
 * The {@link SessionModule} provides a default implementation that stores the data in local memory.
 * In order to persist session data in the store of your choice, simply override the binding for this type with your own implementation.
 * <p>
 * The store methods return {@link Promise} and {@link Operation} in order to support non blocking IO.
 * <p>
 * The store should not make any attempt to interpret the bytes that it is storing/loading.
 *
 * <h3>Example implementation</h3>
 * <p>
 * Here is an example implementation that uses files on the filesystem to store session data.
 * <pre class="java">{@code
 * import com.google.common.io.Files;
 * import com.google.inject.Singleton;
 * import io.netty.buffer.ByteBuf;
 * import io.netty.buffer.ByteBufAllocator;
 * import io.netty.buffer.ByteBufInputStream;
 * import io.netty.buffer.ByteBufOutputStream;
 * import io.netty.util.AsciiString;
 * import ratpack.exec.Operation;
 * import ratpack.exec.Promise;
 * import ratpack.exec.Blocking;
 * import ratpack.guice.ConfigurableModule;
 * import ratpack.guice.Guice;
 * import ratpack.session.Session;
 * import ratpack.session.SessionModule;
 * import ratpack.session.SessionStore;
 * import ratpack.test.embed.EphemeralBaseDir;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import javax.inject.Inject;
 * import java.io.File;
 * import java.io.IOException;
 * import java.util.Arrays;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   static class FileSessionStore implements SessionStore {
 *     private final ByteBufAllocator bufferAllocator;
 *     private final File dir;
 *
 *     {@literal @}Inject
 *     public FileSessionStore(ByteBufAllocator bufferAllocator, FileSessionModule.Config config) {
 *       this.bufferAllocator = bufferAllocator;
 *       this.dir = config.dir;
 *     }
 *
 *     {@literal @}Override
 *     public void onStart({@literal @}SuppressWarnings("deprecation") ratpack.server.StartEvent event) throws Exception {
 *       Blocking.op(() -> {
 *         assert dir.mkdirs() || dir.exists();
 *       }).then();
 *     }
 *
 *     {@literal @}Override
 *     public void onStop({@literal @}SuppressWarnings("deprecation") ratpack.server.StopEvent event) throws Exception {
 *       Blocking.op(() -> {
 *         Arrays.asList(dir.listFiles()).forEach(File::delete);
 *         dir.delete();
 *       }).then();
 *     }
 *
 *     {@literal @}Override
 *     public Operation store(AsciiString sessionId, ByteBuf sessionData) {
 *       return Blocking.op(() ->
 *           Files.asByteSink(file(sessionId)).writeFrom(new ByteBufInputStream(sessionData))
 *       );
 *     }
 *
 *     {@literal @}Override
 *     public Promise<ByteBuf> load(AsciiString sessionId) {
 *       File sessionFile = file(sessionId);
 *       return Blocking.get(() -> {
 *         if (sessionFile.exists()) {
 *           ByteBuf buffer = bufferAllocator.buffer((int) sessionFile.length());
 *           try {
 *             Files.asByteSource(sessionFile).copyTo(new ByteBufOutputStream(buffer));
 *             return buffer;
 *           } catch (IOException e) {
 *             buffer.release();
 *             throw e;
 *           }
 *         } else {
 *           return bufferAllocator.buffer(0, 0);
 *         }
 *       });
 *     }
 *
 *     private File file(AsciiString sessionId) {
 *       return new File(dir, sessionId.toString());
 *     }
 *
 *     {@literal @}Override
 *     public Operation remove(AsciiString sessionId) {
 *       return Blocking.op(() -> file(sessionId).delete());
 *     }
 *
 *     {@literal @}Override
 *     public Promise<Long> size() {
 *       return Blocking.get(() -> (long) dir.listFiles(File::isFile).length);
 *     }
 *   }
 *
 *   public static class FileSessionModule extends ConfigurableModule<FileSessionModule.Config> {
 *     public static class Config {
 *       File dir;
 *     }
 *
 *     {@literal @}Override
 *     protected void configure() {
 *       bind(SessionStore.class).to(FileSessionStore.class).in(Singleton.class);
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EphemeralBaseDir.tmpDir().use(baseDir -> {
 *       EmbeddedApp.of(s -> s
 *           .registry(Guice.registry(b -> b
 *               .module(SessionModule.class)
 *               .module(FileSessionModule.class, c -> c.dir = baseDir.getRoot().toFile())
 *           ))
 *           .handlers(c -> c
 *               .get("set/:name/:value", ctx ->
 *                   ctx.get(Session.class).getData().then(sessionData -> {
 *                     sessionData.set(ctx.getPathTokens().get("name"), ctx.getPathTokens().get("value"));
 *                     ctx.render("ok");
 *                   })
 *               )
 *               .get("get/:name", ctx ->
 *                   ctx.render(ctx.get(Session.class).getData().map(sessionData -> sessionData.require(ctx.getPathTokens().get("name"))))
 *               )
 *           )
 *       ).test(httpClient -> {
 *         assertEquals("ok", httpClient.getText("set/foo/bar"));
 *         assertEquals("bar", httpClient.getText("get/foo"));
 *       });
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see SessionModule
 */
@SuppressWarnings("deprecation")
public interface SessionStore extends ratpack.server.Service {

  /**
   * Writes the session data for the given id.
   * <p>
   * The given byte buffer will not be modified by the caller, and will be released by the caller after the returned operation has completed (with error or without).
   *
   * @param sessionId the identifier for the session
   * @param sessionData the session data
   * @return the store operation
   */
  Operation store(AsciiString sessionId, ByteBuf sessionData);

  /**
   * Reads the session data for the given id.
   * <p>
   * The caller will release the promised byte buffer.
   *
   * @param sessionId the identifier for the session
   * @return a promise for the session data
   */
  Promise<ByteBuf> load(AsciiString sessionId);

  /**
   * Removes the session data for the given id.
   *
   * @param sessionId the session id
   * @return the remove operation
   */
  Operation remove(AsciiString sessionId);

  /**
   * The current number of sessions.
   * <p>
   * The exact meaning of this value is implementation dependent.
   * {@code -1} may be returned if the store does not support getting the size.
   *
   * @return a promise for the store size
   */
  Promise<Long> size();
}
