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
import io.netty.util.CharsetUtil;
import ratpack.form.UploadedFile;
import ratpack.http.MediaType;
import ratpack.util.internal.BufferUtil;

public class DefaultUploadedFile implements UploadedFile {

  private final MediaType mediaType;
  private final ByteBuf byteBuf;
  private final String fileName;

  public DefaultUploadedFile(MediaType mediaType, ByteBuf byteBuf, String fileName) {
    this.mediaType = mediaType;
    this.byteBuf = byteBuf;
    this.fileName = fileName;
  }

  @Override
  public MediaType getContentType() {
    return mediaType;
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public ByteBuf getBuffer() {
    return byteBuf;
  }

  @Override
  public byte[] getBytes() {
    return BufferUtil.getBytes(byteBuf);
  }

  @Override
  public String getText() {
    if (mediaType == null) {
      return BufferUtil.getText(byteBuf, CharsetUtil.UTF_8);
    } else {
      return BufferUtil.getText(byteBuf, mediaType);
    }
  }
}
