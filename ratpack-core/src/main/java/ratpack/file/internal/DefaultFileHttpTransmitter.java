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
import ratpack.background.Background;
import ratpack.util.Action;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class DefaultFileHttpTransmitter implements FileHttpTransmitter {

  private final FullHttpRequest request;
  private final HttpResponse response;
  private final Channel channel;

  public DefaultFileHttpTransmitter(FullHttpRequest request, HttpResponse response, Channel channel) {
    this.request = request;
    this.response = response;
    this.channel = channel;
  }

  @Override
  public void transmit(Background background, final BasicFileAttributes basicFileAttributes, final Path file) {
    if (file.getFileSystem().equals(FileSystems.getDefault())) {
      background.exec(new Callable<FileChannel>() {
        public FileChannel call() throws Exception {
          return new FileInputStream(file.toFile()).getChannel();
        }
      }).then(new Action<FileChannel>() {
        public void execute(FileChannel fileChannel) {
          FileRegion defaultFileRegion = new DefaultFileRegion(fileChannel, 0, basicFileAttributes.size());
          transmit(basicFileAttributes, defaultFileRegion);
        }
      });
    } else {
      background.exec(new Callable<ReadableByteChannel>() {
        public ReadableByteChannel call() throws Exception {
          return Files.newByteChannel(file);
        }
      }).then(new Action<ReadableByteChannel>() {
        public void execute(ReadableByteChannel fileChannel) {
          transmit(basicFileAttributes, new ChunkedNioStream(fileChannel));
        }
      });
    }
  }

  private void transmit(BasicFileAttributes basicFileAttributes, final Object message) {
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, basicFileAttributes.size());

    if (isKeepAlive(request)) {
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    request.content().release();

    HttpResponse minimalResponse = new DefaultHttpResponse(response.getProtocolVersion(), response.getStatus());
    minimalResponse.headers().set(response.headers());
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
