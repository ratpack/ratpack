package org.ratpackframework.groovy

import org.ratpackframework.bootstrap.RatpackServer

import org.ratpackframework.test.RatpackSpec

class RatpackGroovySpec extends RatpackSpec {

  boolean compileStatic = false
  boolean reloadable = false

  File getRatpackFile() {
    file("ratpack.groovy")
  }

  File templateFile(String path) {
    file("templates/$path")
  }

  @Override
  RatpackServer createApp() {
    RatpackScriptApp.ratpack(ratpackFile, 0, null, compileStatic, reloadable)
  }

}
