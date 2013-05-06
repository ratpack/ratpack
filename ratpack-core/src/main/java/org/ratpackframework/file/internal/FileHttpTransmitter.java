package org.ratpackframework.file.internal;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.ratpackframework.http.internal.HttpDateUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;

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

    response.setHeader(CONTENT_LENGTH, fileLength);
    response.setHeader(LAST_MODIFIED, HttpDateUtil.formatDate(new Date(targetFile.lastModified())));

    // Write the initial line and the header.
    if (!channel.isOpen()) {
      return false;
    }

    channel.write(response);

    // Write the content.
    ChannelFuture writeFuture;
    final FileRegion region = new DefaultFileRegion(raf.getChannel(), 0, fileLength);
    try {
      writeFuture = channel.write(region);
    } catch (Exception ignore) {
      if (channel.isOpen()) {
        channel.close();
      }
      return false;
    }

    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        future.addListener(ChannelFutureListener.CLOSE);
        region.releaseExternalResources();
      }
    });

    return true;
  }

}
