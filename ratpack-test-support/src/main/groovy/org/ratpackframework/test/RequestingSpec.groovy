package org.ratpackframework.test

import com.jayway.restassured.RestAssured
import com.jayway.restassured.response.Cookie
import com.jayway.restassured.response.Cookies
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.RequestSpecification
import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.groovy.Closures
import org.spockframework.lang.ConditionBlock
import spock.lang.Specification

abstract class RequestingSpec extends Specification {

  RequestSpecification request = createRequest()
  Response response
  RatpackServer server
  List<Cookie> cookies = []

  abstract protected RatpackServer createServer()

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
    startServerIfNeeded()
  }

  String postText(String path = "") {
    post(path)
    response.asString()
  }

  Response put(String path = "") {
    startServerIfNeeded()
    response = request.put(toUrl(path))
    postRequest()
  }

  String putText(String path = "") {
    put(path).asString()
  }

  String toUrl(String path) {
    if (!server) {
      throw new IllegalStateException("Server not started")
    }

    "http://$server.bindHost:$server.bindPort/$path"
  }

  void stopServer() {
    server?.stopAndWait()
    server = null
  }

  def cleanupSpec() {
    stopServer()
  }

  protected startServerIfNeeded() {
    if (!server) {
      server = createServer()
      server.start()
    }
  }

  void request(@DelegatesTo(value = RequestSpecification, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    Closures.configure(request, closure)
  }

  @ConditionBlock
  void response(@DelegatesTo(value = Response, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    Closures.configure(response, closure)
  }

}
