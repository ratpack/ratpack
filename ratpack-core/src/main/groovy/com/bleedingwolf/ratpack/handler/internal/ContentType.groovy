package com.bleedingwolf.ratpack.handler.internal

class ContentType {

  final String base
  final Map<String, String> params

  ContentType(String headerValue) {
    headerValue = headerValue?.trim()
    if (!headerValue) {
      base = null
      params = Collections.emptyMap()
    } else {
      String[] parts = headerValue.tokenize(';')
      base = parts[0]
      if (parts.size() > 1) {
        params = [:]
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
