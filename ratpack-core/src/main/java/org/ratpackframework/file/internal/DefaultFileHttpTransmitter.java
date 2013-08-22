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

package org.ratpackframework.file.internal;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.ratpackframework.block.Blocking;
import org.ratpackframework.util.Action;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class DefaultFileHttpTransmitter implements FileHttpTransmitter {

  private final HttpRequest request;
  private final HttpResponse response;
  private final Channel channel;

  public DefaultFileHttpTransmitter(HttpRequest request, HttpResponse response, Channel channel) {
    this.request = request;
    this.response = response;
    this.channel = channel;
  }

  @Override
  public void transmit(Blocking blocking, final BasicFileAttributes basicFileAttributes, final File file) {
    blocking.exec(new Callable<FileChannel>() {
      @Override
      public FileChannel call() throws Exception {
        return new FileInputStream(file).getChannel();
      }
    }).then(new Action<FileChannel>() {
      @Override
      public void execute(FileChannel fileChannel) {
        transmit(basicFileAttributes, fileChannel);
      }
    });
  }

  private void transmit(BasicFileAttributes basicFileAttributes, final FileChannel fileChannel) {
    long length = basicFileAttributes.size();

    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, length);

    if (isKeepAlive(request)) {
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    if (!channel.isOpen()) {
      closeQuietly(fileChannel);
      return;
    }

    HttpResponse minimalResponse = new DefaultHttpResponse(response.getProtocolVersion(), response.getStatus());
    minimalResponse.headers().set(response.headers());
    channel.write(minimalResponse); // headers

    FileRegion message = new DefaultFileRegion(fileChannel, 0, length);
    ChannelFuture writeFuture = channel.writeAndFlush(message);

    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        try {
          fileChannel.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });

    ChannelFuture lastContentFuture = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    if (!isKeepAlive(response)) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }
  }

}
