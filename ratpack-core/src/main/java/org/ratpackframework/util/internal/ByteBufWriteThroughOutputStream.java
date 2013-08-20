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

package org.ratpackframework.util.internal;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.OutputStream;

public class ByteBufWriteThroughOutputStream extends OutputStream {

  private final ByteBuf byteBuf;

  public ByteBufWriteThroughOutputStream(ByteBuf byteBuf) {
    this.byteBuf = byteBuf;
  }

  @Override
  public void write(int b) throws IOException {
    byteBuf.writeByte(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    byteBuf.writeBytes(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    byteBuf.writeBytes(b, off, len);
  }

}
