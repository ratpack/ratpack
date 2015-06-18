/*
 * Copyright 2015 the original author or authors.
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

package ratpack.pac4j.cookiesession

import groovy.io.GroovyPrintStream
import org.pac4j.core.profile.UserProfile
import org.pac4j.http.client.FormClient
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.http.profile.UsernameProfileCreator
import org.slf4j.LoggerFactory
import ratpack.handling.RequestId
import ratpack.handling.RequestLog
import ratpack.pac4j.RatpackPac4j
import ratpack.session.SessionModule
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION
import static io.netty.handler.codec.http.HttpResponseStatus.FOUND
import static io.netty.handler.codec.http.HttpResponseStatus.OK

class Pac4jSessionSpec extends RatpackGroovyDslSpec {

  def "successful authorization"() {
    given:
    bindings {
      module SessionModule
    }

    handlers {
      all(RatpackPac4j.authenticator(new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator())))
      get("noauth") {
        def userProfile = maybeGet(UserProfile).orElse(null)
        response.send "noauth:" + userProfile?.attributes?.username
      }
      prefix("auth") {
        all(RatpackPac4j.requireAuth(FormClient))
        get {
          def userProfile = maybeGet(UserProfile).orElse(null)
          response.send "auth:" + userProfile?.attributes?.username
        }
      }
      get("login") {
        def userProfile = maybeGet(UserProfile).orElse(null)
        response.send "login:" + userProfile?.attributes?.username
      }
    }

    when: "request a page that requires authentication"
    requestSpec {
      it.redirects(0)
    }
    def resp1 = get("auth")

    then: "the request is redirected to login page"
    resp1.statusCode == FOUND.code()
    resp1.headers.get(LOCATION).contains("/login")

    when: "follow the redirect"
    def resp2 = get(resp1.headers.get(LOCATION))

    then: "the response is redirected to login form"
    resp2.statusCode == OK.code()
    resp2.body.text == "login:null"

    when: "send authorization request"
    def resp3 = get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=foo&password=foo&client_name=FormClient")

    then: "the response is redirected to auth page"
    resp3.statusCode == FOUND.code()
    resp3.headers.get(LOCATION).contains("/auth")

    when: "following the redirect"
    def resp4 = get(resp3.headers.get(LOCATION))

    then: "the requested page is returned"
    resp4.statusCode == OK.code()
    resp4.body.text == "auth:foo"

    when: "request the auth page again"
    def resp5 = get("auth")

    then: "the requested page is returned"
    resp5.statusCode == OK.code()
    resp5.body.text == "auth:foo"

    when: "after reseting all the cookies"
    resetRequest()
    requestSpec {
      it.redirects(0)
    }
    def resp6 = get("auth")

    then: "authorization is required"
    resp6.statusCode == FOUND.code()
    resp6.headers.get(LOCATION).contains("login")

    when: "follow the redirect"
    def resp7 = get(resp6.headers.get(LOCATION))
    resp7.statusCode == OK.code()
    resp7.body.text == "login:null"
    def resp8 = get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=bar&password=bar&client_name=FormClient")

    then: "the requested page is returned with new login"
    resp8.statusCode == FOUND.code()
    resp8.headers.get(LOCATION).contains("auth")

    when: "following the redirect"
    def resp9 = get(resp8.headers.get(LOCATION))

    then: "the requested page is returned"
    resp9.statusCode == OK.code()
    resp9.body.text == "auth:bar"
  }

  def "log user id in request log"() {
    def loggerOutput = new ByteArrayOutputStream()
    def logger = LoggerFactory.getLogger(RequestId)
    def originalStream = logger.TARGET_STREAM
    def latch = new CountDownLatch(1)
    logger.TARGET_STREAM = new GroovyPrintStream(loggerOutput) {

      @Override
      void println(String s) {
        super.println(s)
        originalStream.println(s)
        if (s.contains(RequestLog.simpleName)) {
          latch.countDown()
        }
      }
    }

    given:
    bindings {
      module SessionModule
    }
    handlers {
      all RequestId.bindAndLog()
      all RatpackPac4j.authenticator(new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator()))
      prefix("foo") {
        all(RatpackPac4j.requireAuth(FormClient))
        get {
          render request.get(RequestId).id
        }
      }
      get("login") {
        def userProfile = maybeGet(UserProfile).orElse(null)
        response.send "login:" + userProfile?.attributes?.username
      }
    }

    when: "request a page that requires authentication"
    requestSpec {
      it.redirects(0)
    }
    def resp1 = get("foo")

    then: "the request is redirected to login page"
    resp1.statusCode == FOUND.code()
    resp1.headers.get(LOCATION).contains("/login")

    when: "follow the redirect"
    def resp2 = get(resp1.headers.get(LOCATION))

    then: "the response is redirected to login form"
    resp2.statusCode == OK.code()
    resp2.body.text == "login:null"

    when: "send authorization request"
    def resp3 = get("$RatpackPac4j.DEFAULT_AUTHENTICATOR_PATH?username=foo&password=foo&client_name=FormClient")

    then: "the response is redirected to auth page"
    resp3.statusCode == FOUND.code()
    resp3.headers.get(LOCATION).contains("/foo")

    when: "following the redirect"
    def resp4 = get(resp3.headers.get(LOCATION))

    then: "the requested page is returned"
    resp4.statusCode == OK.code()

    then: 'the request is logged with the user id'
    latch.await(5, TimeUnit.SECONDS)
    String output = loggerOutput.toString()
    output ==~ /(?s).*INFO ratpack\.handling\.RequestLog - 127\.0\.0\.1 - foo \[.*\] "GET \/foo HTTP\/1\.1" 200 36 id=.*/
  }
}
