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
	
	@Test
	void map() {
		requestParams << ["key[a]","val1"]
		requestParams << ["key[b]","val2"]
		def params = read()
		assertEquals("val1", params.key["a"])
		assertEquals("val2", params.key["b"])
	}
	
	@Test
	void nestedMap() {
		requestParams << ["key[a][a]","val1"]
		requestParams << ["key[a][b]","val2"]
		def params = read()
		assertEquals("val1", params.key["a"]["a"])
		assertEquals("val2", params.key["a"]["b"])
	}
	
	@Test
	void arrayWithinNestedMap() {
		requestParams << ["key[a][b][]","val1"]
		requestParams << ["key[a][b][]","val2"]
		def params = read()
		assertEquals("val1", params.key["a"]["b"][0])
		assertEquals("val2", params.key["a"]["b"][1])
	}
	
	@Test
	void mapWithinArrayDifferentKeyAddsToSameMap() {
		requestParams << ["a[]a","val1"]
		requestParams << ["a[]b","val2"]
		def params = read()
		assertEquals("val1", params.a[0].a)
		assertEquals("val2", params.a[0].b)
	}
	
	@Test
	void mapWithinArraySameKeyGetsNewMap() {
		requestParams << ["a[]a","val1"]
		requestParams << ["a[]a","val2"]
		def params = read()
		assertEquals("val1", params.a[0].a)
		assertEquals("val2", params.a[1].a)
	}
	
	@Test
	void subscripts() {
		assertEquals(["a"],reader.subscripts("a"))
		assertEquals(["a"],reader.subscripts("[a]"))
		assertEquals(["a","b"],reader.subscripts("[a][b]"))
		assertEquals(["a","b"],reader.subscripts("[a]b"))
		assertEquals(["a",""],reader.subscripts("[a][]"))
	}
	
	@Test
	void badlyFormedSubscripts() {
		assertEquals(["a["],reader.subscripts("a["))
		assertEquals(["[b][]]"],reader.subscripts("[b][]]"))
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
        reader.readRequestParams(request)
    }

}
