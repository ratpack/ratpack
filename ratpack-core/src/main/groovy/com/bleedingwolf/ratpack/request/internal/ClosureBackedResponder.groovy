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

package com.bleedingwolf.ratpack.request.internal

import com.bleedingwolf.ratpack.request.Request
import com.bleedingwolf.ratpack.request.Response

import com.bleedingwolf.ratpack.request.ResponderDsl

class ClosureBackedResponder extends AbstractResponder {

  private final Closure<?> closure

  ClosureBackedResponder(Request request, Closure<?> closure) {
    super(request)
    this.closure = closure
  }

  @Override
  void respond(Response response) {
    Closure<?> clone = closure.clone() as Closure<?>
    clone.delegate = new ResponderDsl(getRequest(), response)
    clone.resolveStrategy = Closure.DELEGATE_FIRST

    switch (clone.maximumNumberOfParameters) {
      case 0:
        clone.call()
        break
      case 1:
        clone.call(getRequest())
        break
      default:
        clone.call(getRequest(), response)
    }

    clone.delegate = null
  }
}
