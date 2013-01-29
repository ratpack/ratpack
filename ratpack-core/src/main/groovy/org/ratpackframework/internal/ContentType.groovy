package org.ratpackframework.internal

class ContentType {

  final String base
  final Map<String, String> params

  ContentType(String headerValue) {
    headerValue = headerValue?.trim()
    if (!headerValue) {
      base = null
      params = Collections.emptyMap()
    } else {
      params = [:]
      String[] parts = headerValue.tokenize(';')
      base = parts[0]
      if (parts.size() > 1) {
        for (part in parts[1..-1]) {
          def (key, value) = part.split("=", 2)
          params.put(key, value.trim())
        }
      }
    }
  }

  String getCharset() {
    params["charset"] ?: 'ISO-8859-1'
  }

}
