package com.bleedingwolf.ratpack
import org.json.JSONObject

public class RatpackRequestDelegate {

    def renderer

    def params = [:]
    def urlparams = [:]
    def headers = [:]

    def request = null
    def response = null

    void setHeader(name, value) {
        response.setHeader(name.toString(), value.toString())
    }
    
    void setRequest(req) {
        request = req
        
        req.parameterNames.each { p ->
            def values = req.getParameterValues(p)
            if(values.length == 1)
                values = values[0]
            params[p] = values
        }
        
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
