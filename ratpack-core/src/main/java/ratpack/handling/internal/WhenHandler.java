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

package ratpack.handling.internal;

import ratpack.func.Predicate;
import ratpack.handling.Context;
import ratpack.handling.Handler;

public class WhenHandler implements Handler {

  private final Predicate<? super Context> test;
  private final Handler[] handler;

  public WhenHandler(Predicate<? super Context> test, Handler handler) {
    this.test = test;
    this.handler = ChainHandler.unpack(handler);
  }

  @Override
  public void handle(Context ctx) throws Exception {
    if (test.apply(ctx)) {
      ctx.insert(handler);
    } else {
      ctx.next();
    }
  }
}
