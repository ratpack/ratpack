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

package ratpack.file.internal;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedNioStream;
import ratpack.exec.ExecContext;
import ratpack.func.Action;
import ratpack.http.internal.CustomHttpResponse;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.util.internal.NumberUtil;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class DefaultFileHttpTransmitter implements FileHttpTransmitter {

  private final FullHttpRequest request;
  private final HttpHeaders httpHeaders;
  private final Channel channel;
  private final boolean compress;
  private final long startTime;

  public DefaultFileHttpTransmitter(FullHttpRequest request, HttpHeaders httpHeaders, Channel channel, boolean compress, long startTime) {
    this.request = request;
    this.httpHeaders = httpHeaders;
    this.channel = channel;
    this.compress = compress;
    this.startTime = startTime;
  }

  private static boolean isNotNullAndStartsWith(String value, String... prefixes) {
    if (value == null) {
      return false;
    }
    for (String prefix : prefixes) {
      if (value.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void transmit(ExecContext execContext, final BasicFileAttributes basicFileAttributes, final Path file) throws Exception {
    final boolean compressThis = compress && basicFileAttributes.size() > 1024 && isNotNullAndStartsWith(httpHeaders.get(HttpHeaderConstants.CONTENT_TYPE), "text/", "application/");

    if (compress && !compressThis) {
      // Signal to the compressor not to compress this
      httpHeaders.set(HttpHeaderConstants.CONTENT_ENCODING, HttpHeaders.Values.IDENTITY);
    }

    if (file.getFileSystem().equals(FileSystems.getDefault()) && !compressThis) {
      execContext.blocking(new Callable<FileChannel>() {
        public FileChannel call() throws Exception {
          return new FileInputStream(file.toFile()).getChannel();
        }
      }).then(new Action<FileChannel>() {
        public void execute(FileChannel fileChannel) throws Exception {
          FileRegion defaultFileRegion = new DefaultFileRegion(fileChannel, 0, basicFileAttributes.size());
          transmit(basicFileAttributes, defaultFileRegion);
        }
      });
    } else {
      execContext.blocking(new Callable<ReadableByteChannel>() {
        public ReadableByteChannel call() throws Exception {
          return Files.newByteChannel(file);
        }
      }).then(new Action<ReadableByteChannel>() {
        public void execute(ReadableByteChannel fileChannel) throws Exception {
          transmit(basicFileAttributes, new ChunkedInputAdapter(new ChunkedNioStream(fileChannel)));
        }
      });
    }
  }

  private void transmit(BasicFileAttributes basicFileAttributes, final Object message) {
    HttpResponse response = new CustomHttpResponse(HttpResponseStatus.OK, httpHeaders);
    response.headers().set(HttpHeaderConstants.CONTENT_LENGTH, basicFileAttributes.size());

    if (isKeepAlive(request)) {
      response.headers().set(HttpHeaderConstants.CONNECTION, HttpHeaderConstants.KEEP_ALIVE);
    }

    request.content().release();

    HttpResponse minimalResponse = new DefaultHttpResponse(response.getProtocolVersion(), response.getStatus());
    minimalResponse.headers().set(response.headers());
    long stopTime = System.nanoTime();

    if (startTime > 0) {
      minimalResponse.headers().set("X-Response-Time", NumberUtil.toMillisDiffString(startTime, stopTime));
    }

    ChannelFuture writeFuture = channel.writeAndFlush(minimalResponse);

    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
          channel.close();
        }
      }
    });

    writeFuture = channel.write(message);

    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
          channel.close();
        }
      }
    });

    ChannelFuture lastContentFuture = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    if (!isKeepAlive(response)) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

}
