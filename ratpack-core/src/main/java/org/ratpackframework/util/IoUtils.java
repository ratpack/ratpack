package org.ratpackframework.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public abstract class IoUtils {

  public static ByteBuf readFile(File file) throws IOException {
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

    return Unpooled.wrappedBuffer(mBuf);
  }

  public static ByteBuf utf8Buffer(String str) {
    return ByteBuf(utf8Bytes(str));
  }

  public static byte[] utf8Bytes(String str) {
    return str.getBytes(CharsetUtil.UTF_8);
  }

  public static String utf8String(ByteBuf ByteBuf) {
    return ByteBuf.toString(CharsetUtil.UTF_8);
  }

  public static ByteBuf ByteBuf(byte[] bytes) {
    return Unpooled.wrappedBuffer(bytes);
  }

}
