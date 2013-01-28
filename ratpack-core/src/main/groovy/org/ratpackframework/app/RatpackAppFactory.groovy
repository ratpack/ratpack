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

package org.ratpackframework.app

import org.ratpackframework.templating.TemplateRenderer
import org.ratpackframework.routing.internal.ScriptBackedRouter
import groovy.transform.CompileStatic
import org.vertx.java.core.Vertx

@CompileStatic
public class RatpackAppFactory {

  RatpackApp create(Vertx vertx, File configFile) {
    def baseDir = configFile.parentFile
    def config = new Config()

    def publicDir = new File(baseDir, config.publicDir)
    def templateRenderer = new TemplateRenderer(vertx, new File(baseDir, config.templatesDir), config.templateCacheSize)
    def router = new ScriptBackedRouter(vertx, new File(baseDir, config.routes), templateRenderer)

    new RatpackApp(vertx, config.port, "/", router, templateRenderer, publicDir)
  }

}
