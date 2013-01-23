/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.routing.internal

import org.ratpackframework.responder.Responder
import org.ratpackframework.routing.Router
import org.ratpackframework.routing.RouterBuilder
import org.ratpackframework.script.internal.ScriptRunner
import org.ratpackframework.templating.TemplateRenderer
import org.vertx.java.core.http.HttpServerRequest

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class ScriptBackedRouter implements Router {

  private final File scriptFile
  private final TemplateRenderer templateRenderer

  private final AtomicLong lastModified = new AtomicLong(-1)
  private final AtomicReference<byte[]> content = new AtomicReference<byte[]>()
  private final AtomicReference<Router> router = new AtomicReference<>(null)
  private final Lock lock = new ReentrantLock()

  ScriptBackedRouter(File scriptFile, TemplateRenderer templateRenderer) {
    this.scriptFile = scriptFile
    this.templateRenderer = templateRenderer
  }

  @Override
  Responder route(HttpServerRequest request) {
    if (!scriptFile.exists()) {
      return null
    }

    checkForChanges()
    router.get().route(request)
  }

  private void checkForChanges() {
    if (isNeedUpdate()) {
      lock.lock()
      try {
        if (isNeedUpdate()) {
          refresh()
        }
      } finally {
        lock.unlock()
      }
    }
  }

  private void refresh() {
    List<Router> routers = []
    def routerBuilder = new RouterBuilder(routers, templateRenderer)
    long lastModified = scriptFile.lastModified()
    byte[] bytes = scriptFile.bytes
    String string = new String(bytes)
    new ScriptRunner().run(string, routerBuilder)
    router.set(new CompositeRouter(routers))
    this.lastModified.set(lastModified)
    this.content.set(bytes)
  }

  private boolean isNeedUpdate() {
    router.get() == null || isTimestampMismatch() || isContentMismatch()
  }

  private boolean isTimestampMismatch() {
    scriptFile.lastModified() != lastModified.get()
  }

  private boolean isContentMismatch() {
    scriptFile.bytes != content.get()
  }

}
