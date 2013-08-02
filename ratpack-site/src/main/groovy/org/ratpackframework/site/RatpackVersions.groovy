package org.ratpackframework.site

class RatpackVersions {

  final String current
  final String snapshot

  RatpackVersions(Properties properties) {
    assert properties.getProperty("current")
    assert properties.getProperty("snapshot")

    current = properties.getProperty("current")
    snapshot = properties.getProperty("snapshot")
  }

}
