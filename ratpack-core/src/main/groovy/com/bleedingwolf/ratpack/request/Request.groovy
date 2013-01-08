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

package com.bleedingwolf.ratpack.request

import groovy.json.JsonSlurper

import javax.servlet.http.HttpServletRequest
import com.bleedingwolf.ratpack.internal.ParamParser

class Request {

  final HttpServletRequest servletRequest
  final Map<String, String> urlParams

  @Lazy byte[] input = servletRequest.inputStream.bytes
  @Lazy Map<String, ?> queryParams = new ParamParser().parse(servletRequest.queryString, servletRequest.getCharacterEncoding())
  @Lazy String text = servletRequest.characterEncoding ? new String(input, servletRequest.characterEncoding) : new String(input, 'ISO-8859-1')
  @Lazy Object json = new JsonSlurper().parseText(getText())
  @Lazy Map<String, ?> params = new ParamParser().parse(getText(), servletRequest.getCharacterEncoding())

  Request(HttpServletRequest servletRequest, Map<String, String> urlParams) {
    this.servletRequest = servletRequest
    this.urlParams = urlParams
  }

}
