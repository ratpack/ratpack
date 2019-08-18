/*
 * Copyright 2017 the original author or authors.
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

package ratpack.exec.util;

import ratpack.exec.Promise;
import ratpack.exec.util.internal.DefaultReadWriteAccess;

import java.time.Duration;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Provides read/write serialization, analogous to {@link ReadWriteLock}.
 * <p>
 * Can be used whenever a “resource” has safe concurrent usages and mutually exclusive usages,
 * such as updating a file.
 * <p>
 * The {@link #read(Promise)} and {@link #write(Promise)} methods decorate promises with serialization.
 * Read serialized promises may execute concurrently with other read serialized promises,
 * but not with write serialized promises.
 * Write serialized promises may not execute concurrently with read or write serialized promises.
 * <p>
 * Access is generally fair.
 * That is, access is granted in the order that promises execute (n.b. not in the order they are decorated).
 * <p>
 * Access is not reentrant.
 * Deadlocks are not detected or prevented.
 *
 * <pre class="java">{@code
 * import com.google.common.io.Files;
 * import io.netty.buffer.ByteBufAllocator;
 * import ratpack.exec.Promise;
 * import ratpack.exec.util.ParallelBatch;
 * import ratpack.exec.util.ReadWriteAccess;
 * import ratpack.file.FileIo;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.test.embed.EphemeralBaseDir;
 * import ratpack.test.exec.ExecHarness;
 *
 * import java.nio.charset.Charset;
 * import java.nio.file.Path;
 * import java.util.ArrayList;
 * import java.util.Collections;
 * import java.util.List;
 * import java.time.Duration;
 *
 * import static java.nio.file.StandardOpenOption.*;
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   public static void main(String... args) throws Exception {
 *     EphemeralBaseDir.tmpDir().use(baseDir -> {
 *       ReadWriteAccess access = ReadWriteAccess.create(Duration.ofSeconds(5));
 *       Path f = baseDir.write("f", "foo");
 *
 *       EmbeddedApp.of(a -> a
 *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
 *         .handlers(c -> {
 *           ByteBufAllocator allocator = c.getRegistry().get(ByteBufAllocator.class);
 *
 *           c.path(ctx ->
 *             ctx.byMethod(m -> m
 *               .get(() ->
 *                 FileIo.read(FileIo.open(f, READ, CREATE), allocator, 8192)
 *                   .apply(access::read)
 *                   .map(b -> b.toString(Charset.defaultCharset()))
 *                   .then(ctx::render)
 *               )
 *               .post(() ->
 *                 FileIo.write(ctx.getRequest().getBodyStream(), FileIo.open(f, WRITE, CREATE, TRUNCATE_EXISTING))
 *                   .apply(access::write)
 *                   .then(written -> ctx.render(written.toString()))
 *               )
 *             )
 *           );
 *         })
 *       ).test(httpClient -> {
 *
 *         // Create a bunch of reads and writes
 *         List<Promise<String>> requests = new ArrayList<>();
 *         for (int i = 0; i < 200; ++i) {
 *           requests.add(Promise.sync(httpClient::getText));
 *         }
 *         for (int i = 0; i < 200; ++i) {
 *           requests.add(Promise.sync(() ->
 *             httpClient.request(r -> r
 *               .post().getBody().text("foo")
 *             ).getBody().getText()
 *           ));
 *         }
 *
 *         // Interleave
 *         Collections.shuffle(requests);
 *
 *         // Execute them in parallel
 *         List<String> results = ExecHarness.yieldSingle(r ->
 *           ParallelBatch.of(requests).yield()
 *         ).getValueOrThrow();
 *
 *         assertEquals("foo", Files.toString(f.toFile(), Charset.defaultCharset()));
 *         assertEquals(400, results.size());
 *       });
 *     });
 *   }
 * }
 *
 * }</pre>
 *
 * @since 1.5
 */
public interface ReadWriteAccess {

  /**
   * Create a new read/write access object with the given default timeout.
   *
   * @param defaultTimeout the default maximum amount of time to wait for access (must not be negative, 0 == infinite)
   * @return a new read/write access object
   */
  static ReadWriteAccess create(Duration defaultTimeout) {
    return new DefaultReadWriteAccess(defaultTimeout);
  }

  /**
   * The default timeout value.
   *
   * @return the default timeout value
   */
  Duration getDefaultTimeout();

  /**
   * Decorates the given promise with read serialization.
   * <p>
   * Read serialized promises may execute concurrently with other read serialized promises,
   * but not with write serialized promises.
   * <p>
   * If access is not granted within the default timeout, the promise will wail with {@link TimeoutException}.
   *
   * @param promise the promise to decorate
   * @param <T> the type of promised value
   * @return a decorated promise
   */
  <T> Promise<T> read(Promise<T> promise);

  /**
   * Decorates the given promise with read serialization and the given timeout.
   * <p>
   * Read serialized promises may execute concurrently with other read serialized promises,
   * but not with write serialized promises.
   * <p>
   * If access is not granted within the given timeout, the promise will wail with {@link TimeoutException}.
   *
   * @param promise the promise to decorate
   * @param timeout the maximum amount of time to wait for access (must not be negative, 0 == infinite)
   * @param <T> the type of promised value
   * @return a decorated promise
   */
  <T> Promise<T> read(Promise<T> promise, Duration timeout);

  /**
   * Decorates the given promise with write serialization.
   * <p>
   * Write serialized promises may not execute concurrently with read or write serialized promises.
   * <p>
   * If access is not granted within the default timeout, the promise will wail with {@link TimeoutException}.
   *
   * @param promise the promise to decorate
   * @param <T> the type of promised value
   * @return a decorated promise
   */
  <T> Promise<T> write(Promise<T> promise);

  /**
   * Decorates the given promise with write serialization.
   * <p>
   * Write serialized promises may not execute concurrently with read or write serialized promises.
   * <p>
   * If access is not granted within the given timeout, the promise will wail with {@link TimeoutException}.
   *
   * @param promise the promise to decorate
   * @param timeout the maximum amount of time to wait for access (must not be negative, 0 == infinite)
   * @param <T> the type of promised value
   * @return a decorated promise
   */
  <T> Promise<T> write(Promise<T> promise, Duration timeout);

  /**
   * Thrown if access could not be acquired within the given timeout value.
   */
  class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
      super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  }

}
