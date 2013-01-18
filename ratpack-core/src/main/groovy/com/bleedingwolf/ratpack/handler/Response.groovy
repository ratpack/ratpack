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

package com.bleedingwolf.ratpack.handler

interface Response {

  Map<String, ?> getHeaders()

  int getStatus()

  void setStatus(int status)

  ByteArrayOutputStream getOutput()

  String getContentType()

  void setContentType(String contentType)

  String render(Map context, String templateName)

  String render(String templateName)

  String renderJson(Object o)

  String renderString(String str)

  /**
   * Sends a temporary redirect response to the client using the specified redirect location URL.
   *
   * @param location the redirect location URL
   */
  void sendRedirect(String location)


}
