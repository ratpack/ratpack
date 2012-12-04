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

package com.bleedingwolf.ratpack.routing.internal

import com.bleedingwolf.ratpack.request.Responder
import javax.servlet.http.HttpServletRequest
import com.bleedingwolf.ratpack.script.internal.ScriptRunner
import com.bleedingwolf.ratpack.routing.internal.CompositeRouter
import com.bleedingwolf.ratpack.routing.Router
import com.bleedingwolf.ratpack.routing.RouterBuilder

class ScriptBackedRouter implements Router {

  private final File scriptFile

  ScriptBackedRouter(File scriptFile) {
    this.scriptFile = scriptFile
  }

  @Override
  Responder route(HttpServletRequest request) {
    List<Router> routers = []
    def routerBuilder = new RouterBuilder(routers)
    new ScriptRunner().run(scriptFile, routerBuilder)
    def compositeRouter = new CompositeRouter(routers)
    compositeRouter.route(request)
  }
}