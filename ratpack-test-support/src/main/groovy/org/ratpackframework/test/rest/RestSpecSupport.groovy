package org.ratpackframework.test.rest

import com.jayway.restassured.RestAssured
import com.jayway.restassured.response.Response
import com.jayway.restassured.specification.RequestSpecification
import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.groovy.Closures
import org.ratpackframework.groovy.RatpackScriptApp
import org.spockframework.lang.ConditionBlock
import spock.lang.Specification

class RestSpecSupport extends Specification {

  RequestSpecification request = RestAssured.with()
  Response response

  RatpackServer ratpackServer

  File ratpackScript = new File("src/ratpack/ratpack.groovy")

  int port = 0

  void startServer() {
    def props = new Properties()
    props["ratpack.port"] = port.toString()
    ratpackServer = RatpackScriptApp.ratpack(ratpackScript, props)
    ratpackServer.start()
  }

  void get(String path = "") {
    startServerIfNeeded()
    response = request.get(toUrl(path))
  }

  void post(String path = "") {
    startServerIfNeeded()
    response = request.post(toUrl(path))
  }

  String toUrl(String path) {
    if (!ratpackServer) {
      throw new IllegalStateException("Server not started")
    }

    "http://$ratpackServer.bindHost:$ratpackServer.bindPort/$path"
  }

  void stopServer() {
    ratpackServer?.stopAndWait()
    ratpackServer = null
  }

  def cleanupSpec() {
    stopServer()
  }

  private startServerIfNeeded() {
    if (!ratpackServer) {
      startServer()
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
