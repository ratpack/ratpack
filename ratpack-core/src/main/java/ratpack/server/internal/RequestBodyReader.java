/*
 * Copyright 2015 the original author or authors.
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

import io.netty.buffer.ByteBuf;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.stream.TransformablePublisher;

public interface RequestBodyReader {

  Block DEFAULT_TOO_LARGE_SENTINEL = () -> {
  };


  long getContentLength();

  void setMaxContentLength(long maxContentLength);

  long getMaxContentLength();

  Promise<? extends ByteBuf> read(Block onTooLarge);

  TransformablePublisher<? extends ByteBuf> readStream();

}
