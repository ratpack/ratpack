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
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;

import java.io.*;
import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;

public class FileHttpTransmitter {

  public boolean transmit(final File targetFile, HttpResponse response, Channel channel) {
    final RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(targetFile, "r");
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException(fnfe);
    }

    long fileLength;
    try {
      fileLength = raf.length();
    } catch (IOException e) {
      closeQuietly(raf);
      throw new RuntimeException(e);
    }

    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, fileLength);
    HttpHeaders.setDateHeader(response, HttpHeaders.Names.LAST_MODIFIED, new Date(targetFile.lastModified()));

    // Write the initial line and the header.
    if (!channel.isOpen()) {
      closeQuietly(raf);
      return false;
    }

    try {
      channel.write(response);
    } catch (Exception e) {
      closeQuietly(raf);
    }

    // Write the content.
    ChannelFuture writeFuture;

    ChunkedFile message;
    try {
      message = new ChunkedFile(raf, 0, fileLength, 8192);
      writeFuture = channel.writeAndFlush(message);
    } catch (Exception ignore) {
      if (channel.isOpen()) {
        channel.close();
      }
      return false;
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
    if (!KEEP_ALIVE.equals(response.headers().get(CONNECTION))) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }

    return true;
  }

  private void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }
  }

}
