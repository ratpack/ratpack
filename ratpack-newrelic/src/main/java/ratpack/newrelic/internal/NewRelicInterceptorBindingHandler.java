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

package ratpack.newrelic.internal;

import com.newrelic.api.agent.Trace;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.NoArgAction;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.newrelic.NewRelicTransaction;

public class NewRelicInterceptorBindingHandler implements Handler {

  private static final NewRelicExecInterceptor INTERCEPTOR = new NewRelicExecInterceptor();

  @Override
  public void handle(Context context) throws Exception {
    context.getRequest().add(NewRelicTransaction.class, new DefaultNewRelicTransaction(context));
    context.addInterceptor(INTERCEPTOR, context::next);
  }

  private static class NewRelicExecInterceptor implements ExecInterceptor {
    @Override
    @Trace(dispatcher = true)
    public void intercept(Execution execution, ExecType execType, NoArgAction continuation) throws Exception {
      execution.get(Request.class).get(NewRelicTransaction.class).init();
      continuation.execute();
    }
  }

}
