package org.ratpackframework.test.groovy

import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.groovy.RatpackScriptApp
import org.ratpackframework.test.RatpackSpec

class RatpackGroovyScriptAppSpec extends RatpackSpec {

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
