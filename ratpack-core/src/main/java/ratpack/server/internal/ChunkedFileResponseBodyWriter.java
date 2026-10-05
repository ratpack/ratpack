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
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.stream.ChunkedNioStream;
import ratpack.exec.Blocking;

import java.nio.file.Files;
import java.nio.file.Path;

class ChunkedFileResponseBodyWriter implements ResponseBodyWriter {
  private final Path file;

  ChunkedFileResponseBodyWriter(Path file) {
    this.file = file;
  }

  @Override
  public ChannelFuture write(Channel channel) {
    ChannelPromise channelPromise = channel.newPromise();
    Blocking.get(() -> Files.newByteChannel(file))
      // The response headers have already been sent, so a failure here must not propagate to the
      // request's error handler (which would attempt a second transmission). Failing the promise
      // instead causes the transmitter to log the failure and close the connection.
      .onError(channelPromise::setFailure)
      .then(fileChannel -> channel.writeAndFlush(new HttpChunkedInput(new ChunkedNioStream(fileChannel)), channelPromise));
    return channelPromise;
  }

}
