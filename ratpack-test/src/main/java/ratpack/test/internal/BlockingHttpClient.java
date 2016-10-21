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

package ratpack.test.internal;

import io.netty.buffer.ByteBuf;
import ratpack.exec.Downstream;
import ratpack.exec.ExecController;
import ratpack.exec.ExecResult;
import ratpack.exec.Result;
import ratpack.func.Action;
import ratpack.http.TypedData;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.internal.DefaultReceivedResponse;
import ratpack.http.internal.ByteBufBackedTypedData;
import ratpack.util.Exceptions;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.netty.buffer.Unpooled.unreleasableBuffer;

public class BlockingHttpClient {

  public ReceivedResponse request(HttpClient httpClient, URI uri, ExecController execController, Duration timeout, Action<? super RequestSpec> action) throws Throwable {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<ExecResult<ReceivedResponse>> result = new AtomicReference<>();

    execController.fork()
      .start(e ->
        httpClient.request(uri, action.prepend(s -> s.readTimeout(Duration.ofHours(1))))
          .map(response -> {
            TypedData responseBody = response.getBody();
            ByteBuf responseBuffer = responseBody.getBuffer();
            ByteBuf heapResponseBodyBuffer = unreleasableBuffer(responseBuffer.isDirect() ? TestByteBufAllocators.LEAKING_UNPOOLED_HEAP.heapBuffer(responseBuffer.readableBytes()).writeBytes(responseBuffer) : responseBuffer.retain());

            return new DefaultReceivedResponse(
              response.getStatus(),
              response.getHeaders(),
              new ByteBufBackedTypedData(heapResponseBodyBuffer, responseBody.getContentType())
            );
          })
          .connect(
            new Downstream<ReceivedResponse>() {
              @Override
              public void success(ReceivedResponse value) {
                result.set(ExecResult.of(Result.success(value)));
                latch.countDown();
              }

              @Override
              public void error(Throwable throwable) {
                result.set(ExecResult.of(Result.error(throwable)));
                latch.countDown();
              }

              @Override
              public void complete() {
                result.set(ExecResult.complete());
                latch.countDown();
              }
            }
          )
      );
    try {
      if (!latch.await(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
        TemporalUnit unit = timeout.getUnits().get(0);
        throw new IllegalStateException("Request to " + uri + " took more than " + timeout.get(unit) + " " + unit.toString() + " to complete");
      }
    } catch (InterruptedException e) {
      throw Exceptions.uncheck(e);
    }

    return result.get().getValueOrThrow();
  }

}
