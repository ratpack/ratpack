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

package org.ratpackframework.responder.internal

import org.ratpackframework.TemplateRenderer
import org.ratpackframework.handler.Request
import org.ratpackframework.handler.Response
import groovy.transform.CompileStatic

@CompileStatic
class ClosureBackedResponder extends AbstractResponder {

  private final Closure<?> closure

  ClosureBackedResponder(Request request, TemplateRenderer templateRenderer, Closure<?> closure) {
    super(request, templateRenderer)
    this.closure = closure
  }

  @Override
  void doRespond(Request request, Response response) {
    Closure<?> clone = closure.clone() as Closure<?>
    clone.delegate = response
    clone.resolveStrategy = Closure.DELEGATE_FIRST

    switch (clone.maximumNumberOfParameters) {
      case 0:
        clone.call()
        break
      case 1:
        clone.call(request)
        break
      default:
        clone.call(request, response)
    }

    clone.delegate = null
  }
}
