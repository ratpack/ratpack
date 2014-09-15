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

package ratpack.form.internal;

import io.netty.buffer.ByteBuf;
import ratpack.form.UploadedFile;
import ratpack.http.MediaType;
import ratpack.http.TypedData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class DefaultUploadedFile implements UploadedFile {

  private final TypedData typedData;
  private final String fileName;

  public DefaultUploadedFile(TypedData typedData, String fileName) {
    this.typedData = typedData;
    this.fileName = fileName;
  }

  @Override
  public MediaType getContentType() {
    return typedData.getContentType();
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public ByteBuf getBuffer() {
    return typedData.getBuffer();
  }

  @Override
  public String getText() {
    return typedData.getText();
  }

  @Override
  public String getText(Charset charset) {
    return typedData.getText(charset);
  }

  @Override
  public byte[] getBytes() {
    return typedData.getBytes();
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    typedData.writeTo(outputStream);
  }

  @Override
  public InputStream getInputStream() {
    return typedData.getInputStream();
  }
}
