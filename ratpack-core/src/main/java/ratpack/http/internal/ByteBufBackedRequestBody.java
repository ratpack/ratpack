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
import ratpack.http.Request;
import ratpack.http.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class ByteBufBackedRequestBody implements RequestBody {

  private final Request request;
  private final ByteBuf byteBuf;

  public ByteBufBackedRequestBody(Request request, ByteBuf byteBuf) {
    this.request = request;
    this.byteBuf = byteBuf;
  }

  @Override
  public ByteBuf getBuffer() {
    return byteBuf;
  }

  @Override
  public String getText() {
    return byteBuf.toString(Charset.forName(request.getContentType().getCharset()));
  }

  @Override
  public byte[] getBytes() {
    if (byteBuf.hasArray()) {
      return byteBuf.array();
    } else {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(byteBuf.writerIndex());
      try {
        writeTo(baos);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return baos.toByteArray();
    }
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
