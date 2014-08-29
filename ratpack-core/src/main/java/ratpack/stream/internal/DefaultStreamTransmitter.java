/*
 * Copyright 2014 the original author or authors.
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

package ratpack.stream.internal;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import ratpack.exec.ExecControl;
import ratpack.file.internal.ResponseTransmitter;

public class DefaultStreamTransmitter implements StreamTransmitter {

  private final ResponseTransmitter transmitter;

  public DefaultStreamTransmitter(ResponseTransmitter transmitter) {
    this.transmitter = transmitter;
  }

  @Override
  public void transmit(final ExecControl execContext, final Publisher<ByteBuf> stream) {
    execContext.stream(stream, transmitter.transmitter());
  }

}
