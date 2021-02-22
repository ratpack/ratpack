/*
 * Copyright 2021 the original author or authors.
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

package ratpack.http.client

import ratpack.exec.ExecController
import ratpack.func.Action
import ratpack.http.client.internal.DefaultHttpClient
import ratpack.test.embed.EmbeddedApp

class HttpClientRef implements HttpClient {

  private final Action<? super HttpClientSpec> conf
  private DefaultHttpClient delegate
  private final EmbeddedApp app

  HttpClientRef(Action<? super HttpClientSpec> conf, EmbeddedApp app) {
    this.conf = conf
    this.app = app
  }

  @Delegate
  DefaultHttpClient getDelegate() {
    if (delegate == null) {
      delegate = of {
        app.server.start()
        it.execController(app.server.registry.get().get(ExecController))
        conf.execute(it)
      } as DefaultHttpClient
    }

    delegate
  }

}
