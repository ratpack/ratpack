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

package ratpack.http.client.internal;

import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;

import java.net.URI;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultHttpClient implements HttpClient {

  private final ExecController execController;
  private final ByteBufAllocator byteBufAllocator;
  private final int maxContentLengthBytes;

  public DefaultHttpClient(ExecController execController, ByteBufAllocator byteBufAllocator, int maxContentLengthBytes) {
    this.execController = execController;
    this.byteBufAllocator = byteBufAllocator;
    this.maxContentLengthBytes = maxContentLengthBytes;
  }

  @Override
  public Promise<ReceivedResponse> get(URI uri, Action<? super RequestSpec> requestConfigurer) {
    return request(uri, requestConfigurer);
  }

  private static class Post implements Action<RequestSpec> {
    @Override
    public void execute(RequestSpec requestSpec) throws Exception {
      requestSpec.method("POST");
    }
  }

  @Override
  public Promise<ReceivedResponse> post(URI uri, Action<? super RequestSpec> action) {
    return request(uri, Action.join(new Post(), action));
  }

  @Override
  public Promise<ReceivedResponse> request(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    final ExecControl execControl = execController.getControl();
    final Execution execution = execControl.getExecution();

    try {
      ContentAggregatingRequestAction requestAction = new ContentAggregatingRequestAction(requestConfigurer, uri, execution, byteBufAllocator, maxContentLengthBytes);
      return execController.getControl().promise(requestAction);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  @Override
  public Promise<StreamedResponse> streamRequest(URI uri, final Action<? super RequestSpec> requestConfigurer) {
    final ExecControl execControl = execController.getControl();
    final Execution execution = execControl.getExecution();

    try {
      ContentStreamingRequestAction requestAction = new ContentStreamingRequestAction(requestConfigurer, uri, execution, byteBufAllocator);
      return execController.getControl().promise(requestAction);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

}
