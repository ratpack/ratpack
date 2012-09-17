package com.bleedingwolf.ratpack

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.mortbay.jetty.HttpHeaders
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest

public class RatpackRequestDelegate {

    def renderer

    def params = [:]
    def urlparams = [:]
    def headers = [:]

    def json = [:]

    def request = null
    def response = null
    def requestParamReader = new RatpackRequestParamReader()
    final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass())

    // Begin public API. Don't break this.
    void setHeader(name, value) {
        response.setHeader(name.toString(), value.toString())
    }

    void contentType(String contentType) {
        setHeader(HttpHeaders.CONTENT_TYPE, contentType)
    }

    void setRequest(requestIn) {
        request = requestIn

        // Important we don't try and read both request params AND body. You only get one go at the InputStream on the request.
        if (request.getHeader(HttpHeaders.CONTENT_TYPE)?.contains(MimeTypes.APPLICATION_JSON)) {
            // :TODO: What if this fails? 500 is sub-optimal.
            json = new JsonSlurper().parseText(getRequestBody(request))
        } else {
            params.putAll(requestParamReader.readRequestParams(request))
        }

        request.headerNames.each { header ->
            def values = []
            values.addAll(request.getHeaders(header))
            headers[header.toLowerCase()] = (values.size() == 1 ? values[0] : values)
        }
    }

    String render(templateName, context = [:]) {
        if (!response.containsHeader(HttpHeaders.CONTENT_TYPE)) {
            contentType(MimeTypes.TEXT_HTML)
        }
        renderer.render(templateName, context)
    }

    String renderJson(o) {
        if (!response.containsHeader(HttpHeaders.CONTENT_TYPE)) {
            contentType(MimeTypes.APPLICATION_JSON)
        }
        new JsonBuilder(o).toString()
    }

    // Everything below this is the private API.
    protected final String getRequestBody(request) {
        def bufferedResult = new StringBuffer()
        try {
            request.getReader().eachLine {line -> bufferedResult << line}
        } catch (e) {
            logger.error(e.message, e)
        }
        bufferedResult.toString()
    }
}
