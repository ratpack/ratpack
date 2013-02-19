package org.ratpackframework.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public abstract class IoUtils {

  public static ChannelBuffer readFile(File file) throws IOException {
    FileInputStream fIn = null;
    FileChannel fChan = null;
    long fSize;
    ByteBuffer mBuf;

    try {
      fIn = new FileInputStream(file);
      fChan = fIn.getChannel();
      fSize = fChan.size();
      mBuf = ByteBuffer.allocate((int) fSize);
      fChan.read(mBuf);
      mBuf.rewind();
    } catch (IOException e) {
      if (fChan != null) {
        fChan.close();
      }
      if (fIn != null) {
        fIn.close();
      }

      throw e;
    }

    return ChannelBuffers.wrappedBuffer(mBuf);
  }

  public static ChannelBuffer utf8Buffer(String str) {
    return channelBuffer(utf8Bytes(str));
  }

  public static byte[] utf8Bytes(String str) {
    return str.getBytes(CharsetUtil.UTF_8);
  }

  public static String utf8String(ChannelBuffer channelBuffer) {
    return channelBuffer.toString(CharsetUtil.UTF_8);
  }

  public static ChannelBuffer channelBuffer(byte[] bytes) {
    return ChannelBuffers.wrappedBuffer(bytes);
  }

}
