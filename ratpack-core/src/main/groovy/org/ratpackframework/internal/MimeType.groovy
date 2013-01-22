package org.ratpackframework.internal

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.MimeTypes

// Augments the list of MIME constants to include application/json (not sure why it doesn't already)
@CompileStatic
enum MimeType {

  TEXT_HTML(MimeTypes.TEXT_HTML),
  TEXT_PLAIN(MimeTypes.TEXT_PLAIN),
  APPLICATION_JSON("application/json")

  final String string

  MimeType(String string) {
    this.string = string
  }

}
