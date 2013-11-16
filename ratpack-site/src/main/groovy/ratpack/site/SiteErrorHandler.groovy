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

package ratpack.site

import groovy.transform.CompileStatic
import ratpack.error.ClientErrorHandler
import ratpack.handling.Context

import static ratpack.groovy.Groovy.groovyTemplate

@CompileStatic
class SiteErrorHandler implements ClientErrorHandler {

  @Override
  void error(Context context, int statusCode) {
    context.response.status(statusCode)
    context.with {
      def message = statusCode == 404 ? "The page you have requested does not exist." : "The request is invalid (HTTP $statusCode)."
      render groovyTemplate("error.html", code: statusCode, message: message)
    }
  }
}
