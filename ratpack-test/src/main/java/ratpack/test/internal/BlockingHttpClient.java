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

import io.netty.buffer.UnpooledByteBufAllocator;
import ratpack.exec.ExecContext;
import ratpack.exec.ExecErrorHandler;
import ratpack.exec.Result;
import ratpack.exec.internal.DefaultExecContext;
import ratpack.func.Action;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.util.ExceptionUtils;

import java.util.concurrent.CountDownLatch;

public class BlockingHttpClient {

  private final LaunchConfig launchConfig;

  public BlockingHttpClient() {
    this.launchConfig = LaunchConfigBuilder
      .noBaseDir()
      .bufferAllocator(UnpooledByteBufAllocator.DEFAULT)
      .build();
  }

  public ReceivedResponse request(String httpUrl, Action<? super RequestSpec> action) throws Throwable {
    final RequestAction requestAction = new RequestAction(httpUrl, action);

    ExecContext execContext = new DefaultExecContext(launchConfig, new ExecErrorHandler() {
      @Override
      public void error(ExecContext execContext, Exception exception) {
        requestAction.setResult(Result.<ReceivedResponse>failure(exception));
      }
    });

    execContext.getExecController().exec(execContext.getSupplier(), requestAction);
    try {
      requestAction.latch.await();
    } catch (InterruptedException e) {
      throw ExceptionUtils.uncheck(e);
    }

    return requestAction.result.getValueOrThrow();
  }

  private static class RequestAction implements Action<ExecContext> {
    private final String httpUrl;
    private final Action<? super RequestSpec> action;

    private final CountDownLatch latch = new CountDownLatch(1);
    private Result<ReceivedResponse> result;

    private RequestAction(String httpUrl, Action<? super RequestSpec> action) {
      this.httpUrl = httpUrl;
      this.action = action;
    }

    private void setResult(Result<ReceivedResponse> result) {
      this.result = result;
      latch.countDown();
    }

    @Override
    public void execute(ExecContext execContext) throws Exception {
      execContext.getHttpClient().request(httpUrl, action)
        .onError(new Action<Throwable>() {
          @Override
          public void execute(Throwable exception) throws Exception {
            setResult(Result.<ReceivedResponse>failure(exception));
          }
        })
        .then(new Action<ReceivedResponse>() {
          @Override
          public void execute(ReceivedResponse response) throws Exception {
            response.getBody().getBuffer().retain();
            setResult(Result.success(response));
          }
        });
    }
  }

}
