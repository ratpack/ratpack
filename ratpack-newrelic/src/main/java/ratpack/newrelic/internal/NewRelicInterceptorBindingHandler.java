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
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.newrelic.NewRelicTransaction;

public class NewRelicInterceptorBindingHandler implements Handler {

  private final Handler delegate;

  public NewRelicInterceptorBindingHandler(Handler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(final Context context) throws Exception {
    context.getRequest().add(NewRelicTransaction.class, new DefaultNewRelicTransaction(context));
    context.addInterceptor(new NewRelicExecInterceptor(context), () -> context.insert(delegate));
  }

  private static class NewRelicExecInterceptor implements ExecInterceptor {
    private final Context context;

    public NewRelicExecInterceptor(Context context) {
      this.context = context;
    }

    @Override
    @Trace(dispatcher = true)
    public void intercept(ExecType execType, Runnable continuation) {
      context.getRequest().get(NewRelicTransaction.class).init();
      continuation.run();
    }
  }

}
