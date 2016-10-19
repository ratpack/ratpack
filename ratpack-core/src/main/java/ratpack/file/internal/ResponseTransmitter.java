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

package ratpack.file.internal;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Subscriber;
import ratpack.func.Action;
import ratpack.handling.RequestOutcome;

import java.nio.file.Path;

public interface ResponseTransmitter {

  void transmit(HttpResponseStatus status, ByteBuf body);

  void transmit(HttpResponseStatus status, Path file);

  Subscriber<ByteBuf> transmitter(HttpResponseStatus status);

  void addOutcomeListener(Action<? super RequestOutcome> action);

  void forceCloseConnection();

}
