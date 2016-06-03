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
import io.netty.buffer.UnpooledByteBufAllocator;
import ratpack.exec.Downstream;
import ratpack.exec.ExecController;
import ratpack.exec.ExecResult;
import ratpack.exec.Result;
import ratpack.exec.internal.DefaultExecController;
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

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;

public class BlockingHttpClient {

  public ReceivedResponse request(URI uri, Duration duration, Action<? super RequestSpec> action) throws Throwable {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<ExecResult<ReceivedResponse>> result = new AtomicReference<>();

    try (ExecController execController = new DefaultExecController(2)) {
      execController.fork()
        .start(e ->
          HttpClient.httpClient(new UnpooledByteBufAllocator(false), Integer.MAX_VALUE, execController)
            .request(uri, action.prepend(s -> s.readTimeout(Duration.ofHours(1))))
            .map(response -> {
              TypedData responseBody = response.getBody();
              ByteBuf responseBuffer = responseBody.getBuffer();
              ByteBuf heapResponseBodyBuffer = unreleasableBuffer(responseBuffer.isDirect() ? copiedBuffer(responseBuffer) : responseBuffer.retain());

              return new DefaultReceivedResponse(
                response.getStatus(),
                response.getHeaders(),
                new ByteBufBackedTypedData(heapResponseBodyBuffer, responseBody.getContentType())
              );
            })
            .connect(
              new Downstream<DefaultReceivedResponse>() {
                @Override
                public void success(DefaultReceivedResponse value) {
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
        if (!latch.await(duration.toNanos(), TimeUnit.NANOSECONDS)) {
          TemporalUnit unit = duration.getUnits().get(0);
          throw new IllegalStateException("Request to " + uri + " took more than " + duration.get(unit) + " " + unit.toString() + " to complete");
        }
      } catch (InterruptedException e) {
        throw Exceptions.uncheck(e);
      }

      return result.get().getValueOrThrow();
    }
  }

}
