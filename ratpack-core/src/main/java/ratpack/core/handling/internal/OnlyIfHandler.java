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

package ratpack.core.handling.internal;

import ratpack.core.handling.Context;
import ratpack.core.handling.Handler;
import ratpack.func.Predicate;

public class OnlyIfHandler implements Handler {

  private final Predicate<? super Context> predicate;
  private final Handler handler;

  public OnlyIfHandler(Predicate<? super Context> predicate, Handler handler) {
    this.predicate = predicate;
    this.handler = handler;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    if (predicate.apply(ctx)) {
      handler.handle(ctx);
    } else {
      ctx.next();
    }
  }
}
