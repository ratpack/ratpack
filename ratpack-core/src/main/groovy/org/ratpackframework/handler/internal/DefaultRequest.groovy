package org.ratpackframework.handler.internal

import org.ratpackframework.handler.Request
import org.ratpackframework.internal.GroovyHttpSession
import org.ratpackframework.internal.ParamParser
import groovy.json.JsonSlurper

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

class DefaultRequest implements Request {

  final HttpServletRequest servletRequest
  final Map<String, String> urlParams

  @Lazy byte[] input = servletRequest.inputStream.bytes
  @Lazy Map<String, ?> queryParams = new ParamParser().parse(servletRequest.queryString, servletRequest.getCharacterEncoding())
  @Lazy String text = servletRequest.characterEncoding ? new String(input, servletRequest.characterEncoding) : new String(input, 'ISO-8859-1')
  @Lazy Object json = new JsonSlurper().parseText(getText())
  @Lazy Map<String, ?> params = new ParamParser().parse(getText(), servletRequest.getCharacterEncoding())

  final HttpSession session

  DefaultRequest(HttpServletRequest servletRequest, Map<String, String> urlParams) {
    this.servletRequest = servletRequest
    this.urlParams = urlParams
    this.session = new GroovyHttpSession(servletRequest)
  }

}
