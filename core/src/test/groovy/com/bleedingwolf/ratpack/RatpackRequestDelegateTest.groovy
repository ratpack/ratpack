package com.bleedingwolf.ratpack

import groovy.mock.interceptor.MockFor
import org.junit.Test

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.junit.Assert.*

class RatpackRequestDelegateTest {

    // Minimally, let's test the public API so we are sure it doesn't break.
    @Test
    void renderDefaultContentType() {
        def delegate = new RatpackRequestDelegate()
        delegate.renderer = [render: {templateName, context -> ""}]
        def mockResponse = new MockFor(HttpServletResponse)
        mockResponse.demand.containsHeader(1) {headerName -> false}
        mockResponse.demand.setHeader(1) {token, value ->
            assertEquals token, "Content-Type"
            assertEquals value, "text/html"
        }
        def mockResponseProxy = mockResponse.proxyInstance()

        delegate.response = mockResponseProxy
        delegate.render("foo", [:])
        mockResponse.verify(mockResponseProxy)
    }

    @Test
    void renderJson() {
        def delegate = new RatpackRequestDelegate()
        def mockResponse = new MockFor(HttpServletResponse)
        mockResponse.demand.containsHeader(1) {headerName -> false}
        mockResponse.demand.setHeader(1) {token, value ->
            assertEquals token, "Content-Type"
            assertEquals value, "application/json"
        }
        def mockResponseProxy = mockResponse.proxyInstance()
        def testJson = """{"foo":"bar"}"""

        delegate.response = mockResponseProxy
        assertEquals testJson, delegate.renderJson(["foo": "bar"])
        mockResponse.verify(mockResponseProxy)
    }

    @Test
    void setRequest_params() {
        def delegate = new RatpackRequestDelegate()
        def paramMap = ["foo": "bar"]
        def mockRequest = new MockFor(HttpServletRequest)
        mockRequest.demand.getHeader(1) {token ->
            assertEquals token, "Content-Type"
            null
        }
        mockRequest.demand.getHeaderNames(1) {[]}
        def mockRequestProxy = mockRequest.proxyInstance()

        def mockParamReader = new MockFor(RatpackRequestParamReader)
        mockParamReader.demand.readRequestParams(1) {req -> paramMap}
        def mockParamReaderProxy = mockParamReader.proxyInstance()
        delegate.requestParamReader = mockParamReaderProxy

        delegate.setRequest(mockRequestProxy)
        mockRequest.verify(mockRequestProxy)
        mockParamReader.verify(mockParamReaderProxy)
    }

    @Test
    void setRequest_json() {
        def delegate = new RatpackRequestDelegate()
        def mockRequest = new MockFor(HttpServletRequest)
        def expectedJson = """{"foo":"bar"}"""
        mockRequest.demand.getHeader(1) {token ->
            assertEquals token, "Content-Type"
            "application/json;utf-8"
        }
        mockRequest.demand.getHeaderNames(1) {[]}
        def mockRequestProxy = mockRequest.proxyInstance()

        delegate.metaClass.getRequestBody = { request ->
            expectedJson
        }
        delegate.response = mockRequestProxy
        delegate.setRequest(mockRequestProxy)

        assertEquals(["foo": "bar"], delegate.json)
        mockRequest.verify(mockRequestProxy)
    }

    @Test
    void setRequest_headers() {
        def delegate = new RatpackRequestDelegate()
        def headers = ["header1_key": ["header1_value1", "header1_value2"], "header2_key": ["header2_value"], "header3_key": []]
        def testHeaderNames = headers.collect {it.key}
        def mockRequest = [headerNames: testHeaderNames,
                getHeaders: {valueName -> headers[valueName]},
                getHeader: {null}]
        delegate.setRequest(mockRequest)

        // If there is only one header value, make sure it is NOT in a list
        assertEquals headers["header1_key"], delegate.headers["header1_key"]
        assertEquals headers["header2_key"][0], delegate.headers["header2_key"]
        assertTrue headers["header3_key"].isEmpty()
    }

}
