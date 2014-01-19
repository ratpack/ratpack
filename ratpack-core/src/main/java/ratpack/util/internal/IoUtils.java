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

package ratpack.util.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class IoUtils {

  public static ByteBuf read(Path path) throws IOException {
    try (SeekableByteChannel seekableByteChannel = Files.newByteChannel(path)) {
      return read(seekableByteChannel);
    }
  }

  public static ByteBuf read(File file) throws IOException {
    try (FileInputStream fIn = new FileInputStream(file)) {
      return read(fIn.getChannel());
    }
  }

  private static ByteBuf read(SeekableByteChannel fChan) throws IOException {
    long fSize = fChan.size();
    ByteBuffer mBuf = ByteBuffer.allocate((int) fSize);
    fChan.read(mBuf);
    mBuf.rewind();
    return Unpooled.wrappedBuffer(mBuf);
  }

  public static ByteBuf utf8Buffer(String str) {
    return Unpooled.copiedBuffer(str, CharsetUtil.UTF_8);
  }

  public static byte[] utf8Bytes(String str) {
    return str.getBytes(CharsetUtil.UTF_8);
  }

  public static String utf8String(ByteBuf byteBuf) {
    return byteBuf.toString(CharsetUtil.UTF_8);
  }

  public static ByteBuf byteBuf(byte[] bytes) {
    return Unpooled.wrappedBuffer(bytes);
  }

  public static ByteBuf writeTo(InputStream inputStream, ByteBuf byteBuf) throws IOException {
    byte[] bytes = new byte[1024]; // completely arbitrary size
    int read = inputStream.read(bytes);
    while (read > 0) {
      byteBuf.writeBytes(bytes, 0, read);
      read = inputStream.read(bytes);
    }
    inputStream.close();
    return byteBuf;
  }

}
