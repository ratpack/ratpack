/*
 * Copyright 2015 the original author or authors.
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
import ratpack.func.Block;
import ratpack.handling.Context;
import ratpack.newrelic.NewRelicTransaction;

public class NewRelicExecInterceptor implements ExecInterceptor {

  public static final NewRelicExecInterceptor INSTANCE = new NewRelicExecInterceptor();

  @Override
  @Trace(dispatcher = true)
  public void intercept(Execution execution, ExecType execType, Block executionSegment) throws Exception {
    execution.maybeGet(Context.class).ifPresent(context -> {
      NewRelicTransaction transaction = execution.maybeGet(NewRelicTransaction.class).orElse(null);
      if (transaction == null) {
        transaction = new DefaultNewRelicTransaction(context);
        execution.add(NewRelicTransaction.class, transaction);
      }
      transaction.init();
    });
    executionSegment.execute();
  }
}
