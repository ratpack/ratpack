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

package ratpack.session.internal;

import ratpack.func.Action;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.Handlers;
import ratpack.registry.Registry;
import ratpack.session.SessionAdapter;

public class StoreSessionIfDirtyHandlerDecorator implements HandlerDecorator {

  @Override
  public Handler decorate(Registry serverRegistry, Handler rest) throws Exception {
    return Handlers.chain(ctx -> {
        ctx.getResponse().beforeSend(responseMetaData -> {
          SessionStatus sessionStatus = ctx.get(SessionStatus.class);
          if (sessionStatus.isDirty()) {
            ctx.get(SessionAdapter.class).save().then(Action.noop());
          }
        });
        ctx.next();
      },
      rest
    );
  }

}
