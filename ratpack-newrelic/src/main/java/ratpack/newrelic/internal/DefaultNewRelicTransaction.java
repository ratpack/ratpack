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

import com.newrelic.api.agent.NewRelic;
import ratpack.handling.Context;
import ratpack.newrelic.NewRelicTransaction;

public class DefaultNewRelicTransaction implements NewRelicTransaction {

  private final Context context;
  private final String name;
  private final long timestamp = System.currentTimeMillis();

  public DefaultNewRelicTransaction(Context context) {
    this.context = context;
    this.name = "/" + context.getRequest().getPath();
  }

  @Override
  public void init() {
    NewRelic.setTransactionName(null, name);
    NewRelic.addCustomParameter("timestamp", timestamp);
    NewRelic.setRequestAndResponse(new RatpackRequest(context.getRequest()), new RatpackResponse(context.getResponse()));
  }
}
