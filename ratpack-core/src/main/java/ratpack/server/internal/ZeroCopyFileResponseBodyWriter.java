/*
 * Copyright 2021 the original author or authors.
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

package ratpack.server.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.LastHttpContent;
import ratpack.exec.Blocking;

import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;

class ZeroCopyFileResponseBodyWriter implements ResponseBodyWriter {

  private static final Set<OpenOption> OPEN_OPTIONS = Collections.singleton(StandardOpenOption.READ);

  private final Path file;
  private final long size;

  ZeroCopyFileResponseBodyWriter(Path file, long size) {
    this.file = file;
    this.size = size;
  }

  @Override
  public ChannelFuture write(Channel channel) {
    ChannelPromise channelPromise = channel.newPromise();
    Blocking.get(() -> FileChannel.open(file, OPEN_OPTIONS))
      .then(fileChannel -> {
        channel.write(new DefaultFileRegion(fileChannel, 0, size), channel.voidPromise());
        channel.write(LastHttpContent.EMPTY_LAST_CONTENT, channelPromise);
        channel.flush();
      });
    return channelPromise;
  }
}
