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

package com.timberglund.ratpack.gradle

class RatpackPluginMeta {

  public static final String DEFAULT_RATPACK_VERSION = "0.7.0-SNAPSHOT"

  public static final String

  final String ratpackVersion
  final String servletApiVersion

  RatpackPluginMeta(String ratpackVersion, String servletApiVersion) {
    this.ratpackVersion = ratpackVersion
    this.servletApiVersion = servletApiVersion
  }

  static RatpackPluginMeta fromResource(ClassLoader classLoader) {
    def resource = classLoader.getResource("ratpack.properties")
    def properties = new Properties()
    resource.withInputStream { properties.load(it) }
    
    String ratpackVersion
    String servletApiVersion

    if (properties["ratpack-version"].startsWith("\${")) {
      // means we loaded from the IDE and the file has not been tokenised.
      ratpackVersion = DEFAULT_RATPACK_VERSION
    } else {
      ratpackVersion = properties["ratpack-version"]
    }

    new RatpackPluginMeta(ratpackVersion, servletApiVersion)
  }

}
