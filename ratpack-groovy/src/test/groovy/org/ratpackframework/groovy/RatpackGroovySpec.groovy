package org.ratpackframework.groovy

import org.ratpackframework.bootstrap.RatpackServer
import org.ratpackframework.groovy.bootstrap.RatpackServerFactory
import org.ratpackframework.groovy.config.Config
import org.ratpackframework.groovy.config.internal.DefaultConfig
import org.ratpackframework.test.RatpackSpec

class RatpackGroovySpec extends RatpackSpec {

  Config config

  File getRatpackFile() {
    file("ratpack.groovy")
  }

  @Override
  File getAssetsDir() {
    config.staticAssets.directory
  }

  File templateFile(String path) {
    prepFile(new File(config.templating.directory, path))
  }

  @Override
  RatpackServer createApp() {
    new RatpackServerFactory().create(config)
  }

  def setup() {
    config = new DefaultConfig(dir)
    config.deployment.port = 0
  }
}
