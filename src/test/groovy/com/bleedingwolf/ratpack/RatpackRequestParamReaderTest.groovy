package com.bleedingwolf.ratpack

import org.junit.Test
import org.junit.Before

import static org.junit.Assert.*

class RatpackRequestParamReaderTest { 
    def reader
    def requestParams

    @Before
    void setup() {
        reader = new RatpackRequestParamReader()
        requestParams = []        
    }

    @Test
    void simpleValuePairs() {
        requestParams << ["key","val"]
		requestParams << ["key2","val2"]
        assertEquals([key:"val",key2:"val2"], read())
    }
	
	@Test
	void arrayWithOneEntry() {
		requestParams << ["key[]","val"]
		def params = read()
		assertEquals("val", params.key[0])
	}
	
	@Test
	void arrayWithTwoEntries() {
		requestParams << ["key[]","val1"]
		requestParams << ["key[]","val2"]
		def params = read()
		assertEquals("val1", params.key[0])
		assertEquals("val2", params.key[1])
	}

    def read() {
        def parameterNames = []
        requestParams.each {rp ->
            if (!parameterNames.contains(rp[0])) { 
                parameterNames << rp[0]
            }
        }
        def request = [
            parameterNames: parameterNames,
            getParameterValues: {p ->
                def vals = []
                requestParams.each {rp ->
                    if (rp[0] == p) {
                        vals << rp[1]
                    }
                }
                vals.toArray()
            }
        ]
        def params = [:]
        reader.readRequestParams(request, params)
        params
    }

}
