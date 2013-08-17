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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import org.ratpackframework.block.Blocking;
import org.ratpackframework.util.Action;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.concurrent.Callable;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;

public class FileHttpTransmitter {

  private final HttpRequest request;
  private final HttpResponse response;
  private final Channel channel;

  public FileHttpTransmitter(HttpRequest request, HttpResponse response, Channel channel) {
    this.request = request;
    this.response = response;
    this.channel = channel;
  }

  public static class FileServingInfo {
    public final long lastModified;
    public final long length;
    public final RandomAccessFile randomAccessFile;

    public FileServingInfo(long lastModified, long length, RandomAccessFile randomAccessFile) {
      this.lastModified = lastModified;
      this.length = length;
      this.randomAccessFile = randomAccessFile;
    }
  }

  public void transmit(final Blocking blocking, final File file) {
    blocking.exec(new Callable<FileServingInfo>() {
      @Override
      public FileServingInfo call() throws Exception {
        long lastModified = file.lastModified();
        long length = file.length();
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

        return new FileServingInfo(lastModified, length, randomAccessFile);
      }
    }).then(new Action<FileServingInfo>() {
      @Override
      public void execute(FileServingInfo fileServingInfo) {
        transmit(fileServingInfo);
      }
    });
  }

  public void transmit(FileServingInfo fileServingInfo) {

    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, fileServingInfo.length);
    HttpHeaders.setDateHeader(response, HttpHeaders.Names.LAST_MODIFIED, new Date(fileServingInfo.lastModified));

    if (isKeepAlive(request)) {
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // Write the initial line and the header.
    if (!channel.isOpen()) {
      closeQuietly(fileServingInfo.randomAccessFile);
      return;
    }

    channel.write(response);

    ChannelFuture writeFuture;
    ChunkedFile message;
    try {
      message = new ChunkedFile(fileServingInfo.randomAccessFile, 0, fileServingInfo.length, 8192);
      writeFuture = channel.writeAndFlush(message);
    } catch (Exception ignore) {
      if (channel.isOpen()) {
        channel.close();
      }
      return;
    }

    final ChunkedFile finalMessage = message;
    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        try {
          finalMessage.close();
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
