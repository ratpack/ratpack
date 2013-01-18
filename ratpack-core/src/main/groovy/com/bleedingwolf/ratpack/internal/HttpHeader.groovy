package com.bleedingwolf.ratpack.internal

import org.eclipse.jetty.http.HttpHeaders

enum HttpHeader {

  LOCATION(HttpHeaders.LOCATION),
  CONTENT_TYPE(HttpHeaders.CONTENT_TYPE)

  final String string

  HttpHeader(String string) {
    this.string = string
  }

}
