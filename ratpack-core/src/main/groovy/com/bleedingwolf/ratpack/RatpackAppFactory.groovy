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

package com.bleedingwolf.ratpack

import com.bleedingwolf.ratpack.routing.internal.ScriptBackedRouter

public class RatpackAppFactory {

  RatpackApp create(File configFile) {
    def baseDir = configFile.parentFile
    def config = new Config()

    def router = new ScriptBackedRouter(new File(baseDir, config.routes))
    def templateRenderer = new TemplateRenderer(new File(baseDir, config.templatesDir))
    def publicDir = new File(baseDir, config.publicDir)

    new RatpackApp(config.port, "/", router, templateRenderer, publicDir)
  }

}
