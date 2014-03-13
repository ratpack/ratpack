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
import ratpack.handling.ProcessingInterceptor;
import ratpack.handling.Context;
import ratpack.newrelic.NewRelicTransaction;

public class NewRelicProcessingInterceptor implements ProcessingInterceptor {

  @Override
  public void init(Context context) {
    context.getRequest().register(NewRelicTransaction.class, new DefaultNewRelicTransaction(context));
  }

  @Override
  @Trace(dispatcher = true)
  public void intercept(Type type, Context context, Runnable continuation) {
    context.get(NewRelicTransaction.class).init();
    continuation.run();
  }

}
