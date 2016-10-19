/*
 * Copyright 2013 the original author or authors.
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

package ratpack.groovy.internal;

import ratpack.func.BiFunction;
import ratpack.func.Factory;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.reload.internal.ReloadableFileBackedFactory;

import java.nio.file.Path;

public class ScriptBackedHandler implements Handler {

  private final Factory<Handler> reloadHandler;
  private final Path script;

  public ScriptBackedHandler(Path script, boolean reloadable, BiFunction<? super Path, ? super String, ? extends Handler> capture) throws Exception {
    this.script = script;
    this.reloadHandler = new ReloadableFileBackedFactory<>(script, reloadable, capture::apply);

    if (reloadable) {
      new Thread(() -> {
        try {
          reloadHandler.create();
        } catch (Exception ignore) {
          // ignore
        }
      }).run();
    } else {
      reloadHandler.create();
    }
  }

  public void handle(Context context) throws Exception {
    Handler handler = reloadHandler.create();
    if (handler == null) {
      context.getResponse().send("script file does not exist:" + script.toAbsolutePath());
    } else {
      handler.handle(context);
    }
  }

}
