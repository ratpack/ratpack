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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.ratpackframework.block.Blocking;
import org.ratpackframework.util.Action;

import java.io.*;
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
    public final long length;
    public final FileInputStream fileInputStream;

    public FileServingInfo(long length, FileInputStream fileInputStream) {
      this.length = length;
      this.fileInputStream = fileInputStream;
    }
  }

  public void transmit(final Blocking blocking, final File file) {
    blocking.exec(new Callable<FileServingInfo>() {
      @Override
      public FileServingInfo call() throws Exception {
        long length = file.length();
        FileInputStream randomAccessFile = new FileInputStream(file);

        return new FileServingInfo(length, randomAccessFile);
      }
    }).then(new Action<FileServingInfo>() {
      @Override
      public void execute(FileServingInfo fileServingInfo) {
        transmit(fileServingInfo);
      }
    });
  }

  public void transmit(final FileServingInfo fileServingInfo) {
    long length = fileServingInfo.length;
    final FileInputStream fileInputStream = fileServingInfo.fileInputStream;

    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, length);

    if (isKeepAlive(request)) {
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    if (!channel.isOpen()) {
      closeQuietly(fileInputStream);
      return;
    }

    channel.write(response); // headers

    FileRegion message = new DefaultFileRegion(fileInputStream.getChannel(), 0, length);
    ChannelFuture writeFuture = channel.writeAndFlush(message);

    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        try {
          fileInputStream.close();
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
