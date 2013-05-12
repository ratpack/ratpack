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
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

public class FileHttpTransmitter {

  public boolean transmit(File targetFile, HttpResponse response, Channel channel) {
    RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(targetFile, "r");
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException(fnfe);
    }

    long fileLength;
    try {
      fileLength = raf.length();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, fileLength);
    HttpHeaders.setDateHeader(response, HttpHeaders.Names.LAST_MODIFIED, new Date(targetFile.lastModified()));

    // Write the initial line and the header.
    if (!channel.isOpen()) {
      return false;
    }

    channel.write(response);

    // Write the content.
    ChannelFuture writeFuture;

    try {

      writeFuture = channel.write(new ChunkedFile(raf, 0, fileLength, 8192));
    } catch (Exception ignore) {
      if (channel.isOpen()) {
        channel.close();
      }
      return false;
    }

    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    });

    return true;
  }

}
