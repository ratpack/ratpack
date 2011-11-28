package com.bleedingwolf.ratpack
import org.json.JSONObject
import org.slf4j.LoggerFactory

public class RatpackRequestDelegate {

    def renderer

    def params = [:]
    def urlparams = [:]
    def headers = [:]

    def request = null
    def response = null
    def requestParamReader = new RatpackRequestParamReader()
    final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass())

    void setHeader(name, value) {
        response.setHeader(name.toString(), value.toString())
    }
    
    void setRequest(req) {
        request = req
        params.putAll(requestParamReader.readRequestParams(req))
        
        req.headerNames.each { header ->
            def values = []
            req.getHeaders(header).each { values << it }
            if(values.size == 1)
                values = values.get(0)
            headers[header.toLowerCase()] = values
        }
    }
    
    String render(templateName, context=[:]) {
        if(!response.containsHeader('Content-Type')) {
            setHeader('Content-Type', 'text/html')
        }
        renderer.render(templateName, context)
    }

    void contentType(String contentType) {
        setHeader("Content-Type",contentType)
    }

    String renderJson(o) {
        if (!response.containsHeader("Content-Type")) {
            contentType("application/json")
        }
        new JSONObject(o).toString()
    }

}
