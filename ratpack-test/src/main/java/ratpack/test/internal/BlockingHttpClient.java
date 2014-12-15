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
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Result;
import ratpack.exec.internal.DefaultExecController;
import ratpack.func.Action;
import ratpack.http.TypedData;
import ratpack.http.client.HttpClients;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.internal.DefaultReceivedResponse;
import ratpack.http.internal.ByteBufBackedTypedData;
import ratpack.util.ExceptionUtils;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BlockingHttpClient {

  public ReceivedResponse request(URI uri, long timeout, TimeUnit timeUnit, Action<? super RequestSpec> action) throws Throwable {
    try (ExecController execController = new DefaultExecController(2)) {
      final RequestAction requestAction = new RequestAction(uri, execController, action);

      execController.getControl().exec()
        .onError(throwable -> requestAction.setResult(Result.<ReceivedResponse>failure(throwable)))
        .start(requestAction::execute);

      try {
        if (!requestAction.latch.await(timeout, timeUnit)) {
          throw new IllegalStateException("Request to " + uri + " took more than " + timeout + " " + timeUnit.name().toLowerCase() + " to complete");
        }
      } catch (InterruptedException e) {
        throw ExceptionUtils.uncheck(e);
      }

      return requestAction.result.getValueOrThrow();
    }
  }

  private static class RequestAction implements Action<Execution> {
    private final URI uri;
    private final ExecController execController;
    private final Action<? super RequestSpec> action;

    private final CountDownLatch latch = new CountDownLatch(1);
    private Result<ReceivedResponse> result;

    private RequestAction(URI uri, ExecController execController, Action<? super RequestSpec> action) {
      this.uri = uri;
      this.execController = execController;
      this.action = action;
    }

    private void setResult(Result<ReceivedResponse> result) {
      this.result = result;
      latch.countDown();
    }

    @Override
    public void execute(Execution execution) throws Exception {
      HttpClients.httpClient(execController, UnpooledByteBufAllocator.DEFAULT, Integer.MAX_VALUE)
        .request(uri, Action.join(s -> s.readTimeout(60, TimeUnit.MINUTES), action))
        .then(response -> {
          TypedData responseBody = response.getBody();
          ByteBuf responseBodyBuffer = responseBody.getBuffer();
          responseBodyBuffer = Unpooled.unreleasableBuffer(responseBodyBuffer.retain());
          ReceivedResponse copiedResponse = new DefaultReceivedResponse(
            response.getStatus(),
            response.getHeaders(),
            new ByteBufBackedTypedData(responseBodyBuffer, responseBody.getContentType())
          );
          setResult(Result.success(copiedResponse));
        });
    }
  }

}
