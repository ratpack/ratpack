package org.ratpackframework.file.internal;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.stream.ChunkedFile;
import org.ratpackframework.http.internal.HttpDateUtil;

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
    response.headers().set(HttpHeaders.Names.LAST_MODIFIED, HttpDateUtil.formatDate(new Date(targetFile.lastModified())));

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
