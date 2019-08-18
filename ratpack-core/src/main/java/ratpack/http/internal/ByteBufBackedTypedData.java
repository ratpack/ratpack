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

package ratpack.http.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import ratpack.http.MediaType;
import ratpack.http.TypedData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class ByteBufBackedTypedData implements TypedData {

  private final ByteBuf byteBuf;
  private final MediaType mediaType;

  public ByteBufBackedTypedData(ByteBuf byteBuf, MediaType mediaType) {
    this.mediaType = mediaType;
    this.byteBuf = byteBuf;
  }

  @Override
  public MediaType getContentType() {
    return mediaType;
  }

  @Override
  public ByteBuf getBuffer() {
    return byteBuf.asReadOnly();
  }

  @Override
  public String getText() {
    return getText(CharsetUtil.UTF_8);
  }

  @Override
  public String getText(Charset charset) {
    if (mediaType == null) {
      return byteBuf.toString(charset);
    } else {
      return byteBuf.toString(Charset.forName(mediaType.getCharset(charset.name())));
    }
  }

  @Override
  public byte[] getBytes() {
    return ByteBufUtil.getBytes(byteBuf);
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    byteBuf.resetReaderIndex();
    byteBuf.readBytes(outputStream, byteBuf.writerIndex());
  }

  @Override
  public InputStream getInputStream() {
    return new ByteBufInputStream(byteBuf);
  }

}
