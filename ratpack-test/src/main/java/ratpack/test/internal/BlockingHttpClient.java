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
import ratpack.exec.Execution;
import ratpack.exec.Result;
import ratpack.func.Action;
import ratpack.http.client.HttpClients;
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

  public ReceivedResponse request(Action<? super RequestSpec> action) throws Throwable {
    final RequestAction requestAction = new RequestAction(launchConfig, action);

    launchConfig.getExecController().getControl().fork(new Action<Execution>() {
      @Override
      public void execute(Execution execution) throws Exception {
        requestAction.execute(execution);
      }
    }, new Action<Throwable>() {
      @Override
      public void execute(Throwable throwable) throws Exception {
        requestAction.setResult(Result.<ReceivedResponse>failure(throwable));
      }
    });

    try {
      requestAction.latch.await();
    } catch (InterruptedException e) {
      throw ExceptionUtils.uncheck(e);
    }

    return requestAction.result.getValueOrThrow();
  }

  private static class RequestAction implements Action<Execution> {
    private final LaunchConfig launchConfig;
    private final Action<? super RequestSpec> action;

    private final CountDownLatch latch = new CountDownLatch(1);
    private Result<ReceivedResponse> result;

    private RequestAction(LaunchConfig launchConfig, Action<? super RequestSpec> action) {
      this.launchConfig = launchConfig;
      this.action = action;
    }

    private void setResult(Result<ReceivedResponse> result) {
      this.result = result;
      latch.countDown();
    }

    @Override
    public void execute(Execution execution) throws Exception {
      HttpClients.httpClient(launchConfig).request(action)
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
