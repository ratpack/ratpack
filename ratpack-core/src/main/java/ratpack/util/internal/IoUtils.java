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

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class IoUtils {

  public static ByteBuf read(ByteBufAllocator allocator, Path path) throws IOException {
    try (SeekableByteChannel sbc = Files.newByteChannel(path);
         InputStream in = Channels.newInputStream(sbc)) {
      int size = Ints.checkedCast(sbc.size());
      ByteBuf byteBuf = allocator.directBuffer(size, size);
      byteBuf.writeBytes(in, size);
      return byteBuf;
    }
  }

}
