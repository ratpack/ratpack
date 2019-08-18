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

package ratpack.file;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.reactivestreams.Publisher;
import ratpack.exec.Blocking;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.file.internal.FileReadingPublisher;
import ratpack.file.internal.FileWritingSubscriber;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.bytebuf.ByteBufStreams;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Utilities for streaming to and from files.
 *
 * @since 1.5
 */
public class FileIo {

  private FileIo() {
  }

  /**
   * Creates a promise for an (open) async file channel.
   * <p>
   * Uses {@link AsynchronousFileChannel#open(Path, Set, ExecutorService, FileAttribute[])},
   * but uses the current execution's event loop as the executor service and no file attributes.
   *
   * @param file The path of the file to open or create
   * @param options Options specifying how the file is opened
   * @see AsynchronousFileChannel#open(Path, Set, ExecutorService, FileAttribute[])
   * @see #open(Path, Set, FileAttribute[])
   * @return a promise for an open async file channel
   */
  public static Promise<AsynchronousFileChannel> open(Path file, OpenOption... options) {
    return open(file, ImmutableSet.copyOf(options));
  }

  /**
   * Creates a promise for an (open) async file channel.
   * <p>
   * Uses {@link AsynchronousFileChannel#open(Path, Set, ExecutorService, FileAttribute[])},
   * but uses the current execution's event loop as the executor service.
   *
   * @param file The path of the file to open or create
   * @param options Options specifying how the file is opened
   * @param attrs An optional list of file attributes to set atomically when creating the file
   * @see AsynchronousFileChannel#open(Path, Set, ExecutorService, FileAttribute[])
   * @see #open(Path, OpenOption...)
   * @return a promise for an open async file channel
   */
  public static Promise<AsynchronousFileChannel> open(Path file, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {
    return Blocking.get(() -> AsynchronousFileChannel.open(file, options, Execution.current().getEventLoop(), attrs));
  }

  /**
   * Writes the bytes of the given publisher to the given file starting at the start, returning the number of bytes written.
   * <p>
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   * <p>
   * As file system writes are expensive,
   * you may want to consider using {@link ByteBufStreams#buffer(Publisher, long, int, ByteBufAllocator)}
   * to “buffer” the data in memory before writing to disk.
   *
   * @param publisher the bytes to write
   * @param file a promise for the file to write to
   * @return a promise for the number of bytes written
   */
  public static Promise<Long> write(Publisher<? extends ByteBuf> publisher, Promise<? extends AsynchronousFileChannel> file) {
    return write(publisher, 0, file);
  }

  /** Writes the bytes of the given publisher to the given file, returning the number of bytes written.
   * <p>
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   * <p>
   * As file system writes are expensive,
   * you may want to consider using {@link ByteBufStreams#buffer(Publisher, long, int, ByteBufAllocator)}
   * to “buffer” the data in memory before writing to disk.
   *
   * @param publisher the bytes to write
   * @param position the position in the file to start writing (must be >= 0)
   * @param file a promise for the file to write to
   * @return a promise for the number of bytes written
   */
  public static Promise<Long> write(Publisher<? extends ByteBuf> publisher, long position, Promise<? extends AsynchronousFileChannel> file) {
    return file.flatMap(fileChannel ->
      Promise.<Long>async(down ->
        publisher.subscribe(new FileWritingSubscriber(fileChannel, position, down))
      )
        .close(Blocking.op(((AsynchronousFileChannel) fileChannel)::close))
    );
  }

  /**
   * Writes the given bytes to the given file, starting at the start.
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   *
   * @param bytes the bytes to write
   * @param file the file to write to
   * @return a write operation
   */
  public static Operation write(ByteBuf bytes, Promise<? extends AsynchronousFileChannel> file) {
    return write(bytes, 0, file);
  }

  /**
   * Writes the given bytes to the given file, starting at the given position.
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   *
   * @param bytes the bytes to write
   * @param position the position in the file to start writing
   * @param file the file to write to
   * @return a write operation
   */
  public static Operation write(ByteBuf bytes, long position, Promise<? extends AsynchronousFileChannel> file) {
    return file.flatMap(channel ->
      Promise.async(down ->
        channel.write(bytes.nioBuffer(), position, null, new CompletionHandler<Integer, Void>() {
          @Override
          public void completed(Integer result, Void attachment) {
            bytes.readerIndex(bytes.readerIndex() + result);
            down.success(null);
          }

          @Override
          public void failed(Throwable exc, Void attachment) {
            down.error(exc);
          }
        })
      )
        .close(bytes::release)
        .close(Blocking.op(((AsynchronousFileChannel) channel)::close))
    ).operation();
  }

  /**
   * Streams the contents of a file.
   * <p>
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   *
   * @param file a promise for the file to write to
   * @param allocator the allocator of byte bufs
   * @param bufferSize the read buffer size (i.e. the size of each emitted buffer)
   * @param start the position in the file to start reading from (must be >= 0)
   * @param stop the position in the file to read up to (any value < 1 is treated as EOF)
   * @see #readStream(Promise, ByteBufAllocator, int)
   * @return a publisher of the byte bufs
   */
  public static TransformablePublisher<ByteBuf> readStream(Promise<? extends AsynchronousFileChannel> file, ByteBufAllocator allocator, int bufferSize, long start, long stop) {
    return new FileReadingPublisher(file, allocator, bufferSize, start, stop);
  }

  /**
   * Streams the entire contents of a file.
   * <p>
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   *
   * @param file a promise for the file to write to
   * @param allocator the allocator of byte bufs
   * @param bufferSize the read buffer size (i.e. the size of each emitted buffer)
   * @see #readStream(Promise, ByteBufAllocator, int, long, long)
   * @return a publisher of the byte bufs
   */
  public static TransformablePublisher<ByteBuf> readStream(Promise<? extends AsynchronousFileChannel> file, ByteBufAllocator allocator, int bufferSize) {
    return new FileReadingPublisher(file, allocator, bufferSize, 0, 0);
  }

  /**
   * Read the contents of a file.
   * <p>
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   *
   * @param file a promise for the file to write to
   * @param allocator the allocator of byte bufs
   * @param bufferSize the read buffer size (i.e. the size of buffer used for each read operation)
   * @see #readStream(Promise, ByteBufAllocator, int)
   * @see #read(Promise, ByteBufAllocator, int)
   * @return a publisher of the byte bufs
   */
  public static Promise<CompositeByteBuf> read(Promise<? extends AsynchronousFileChannel> file, ByteBufAllocator allocator, int bufferSize, long start, long stop) {
    return ByteBufStreams.compose(readStream(file, allocator, bufferSize, start, stop), allocator);
  }

  /**
   * Read the contents of a file from the given start until the given stop.
   * <p>
   * Use {@link #open(Path, Set, FileAttribute[])} to create a file promise.
   * <p>
   * The file channel is closed on success or failure.
   *
   * @param file a promise for the file to write to
   * @param allocator the allocator of byte bufs
   * @param bufferSize the read buffer size (i.e. the size of buffer used for each read operation)
   * @see #readStream(Promise, ByteBufAllocator, int, long, long)
   * @see #read(Promise, ByteBufAllocator, int, long, long)
   * @return a publisher of the byte bufs
   */
  public static Promise<CompositeByteBuf> read(Promise<? extends AsynchronousFileChannel> file, ByteBufAllocator allocator, int bufferSize) {
    return ByteBufStreams.compose(readStream(file, allocator, bufferSize), allocator);
  }

}

