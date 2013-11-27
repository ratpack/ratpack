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
import io.netty.buffer.ByteBufInputStream;
import ratpack.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class BufferUtil {

  public static String getText(ByteBuf byteBuf, String charset) {
    return byteBuf.toString(Charset.forName(charset));
  }

  public static String getText(ByteBuf byteBuf, MediaType mediaType) {
    return getText(byteBuf, mediaType.getCharset());
  }

  public static byte[] getBytes(ByteBuf byteBuf) {
    if (byteBuf.hasArray()) {
      return byteBuf.array();
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(byteBuf.writerIndex());
      try {
        writeTo(byteBuf, baos);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return baos.toByteArray();
    }
  }

  public static void writeTo(ByteBuf byteBuf, OutputStream outputStream) throws IOException {
    byteBuf.resetReaderIndex();
    byteBuf.readBytes(outputStream, byteBuf.writerIndex());
  }

  public static InputStream getInputStream(ByteBuf byteBuf) {
    return new ByteBufInputStream(byteBuf);
  }
}
