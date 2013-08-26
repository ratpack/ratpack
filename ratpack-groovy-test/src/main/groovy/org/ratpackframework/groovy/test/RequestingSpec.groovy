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

package org.ratpackframework.groovy.test

import com.jayway.restassured.RestAssured
import com.jayway.restassured.response.Cookie
import com.jayway.restassured.response.Cookies
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.RequestSpecification
import org.ratpackframework.groovy.Util
import org.ratpackframework.test.ApplicationUnderTest
import org.spockframework.lang.ConditionBlock
import spock.lang.Specification

abstract class RequestingSpec extends Specification {

  RequestSpecification request = createRequest()
  Response response
  List<Cookie> cookies = []

  abstract protected ApplicationUnderTest getApplicationUnderTest()

  RequestSpecification createRequest() {
    RestAssured.with().urlEncodingEnabled(false)
  }

  RequestSpecification resetRequest() {
    request = createRequest()
  }

  Response get(String path = "") {
    preRequest()
    response = request.get(toUrl(path))
    postRequest()
  }

  String getText(String path = "") {
    get(path)
    response.asString()
  }

  Response post(String path = "") {
    preRequest()
    response = request.post(toUrl(path))
    postRequest()
  }

  Response head(String path = "") {
    preRequest()
    response = request.head(toUrl(path))
    postRequest()
  }

  Response postRequest() {
    response.detailedCookies.each { Cookie setCookie ->
      def date = setCookie.getExpiryDate()
      cookies.removeAll { it.name == setCookie.name }
      if (date == null || date > new Date()) {
        cookies << setCookie
      }
    }

    response
  }

  def void preRequest() {
    request.cookies = new Cookies(cookies)
  }

  String postText(String path = "") {
    post(path)
    response.asString()
  }

  Response put(String path = "") {
    response = request.put(toUrl(path))
    postRequest()
  }

  String putText(String path = "") {
    put(path).asString()
  }

  Response delete(String path = "") {
    response = request.delete(toUrl(path))
    postRequest()
  }

  String deleteText(String path = "") {
    delete(path).asString()
  }

  String toUrl(String path) {
    "$applicationUnderTest.address/$path"
  }

  void request(@DelegatesTo(value = RequestSpecification, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    Util.configureDelegateFirst(request, closure)
  }

  @ConditionBlock
  void response(@DelegatesTo(value = Response, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    Util.configureDelegateFirst(response, closure)
  }

}
