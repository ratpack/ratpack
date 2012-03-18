package com.bleedingwolf.ratpack;

public class RatpackRequestParamReader {
	
	
    /**
     * This method will attempt to parse params with subscripts into array and 
     * map structures. For instance:
     *     a=1&b=2 becomes [a:1,b:2] as usual.
     *     a[]=1&a[]=2 becomes [a:[1,2]]
     *     a[x]=1&a[y]=2 becomes [a:[x:1,y:2]]
     *     a[x][]=1&a[x][]=2 becomes [a:[x:[1,2]]]
     * Throws an Exception if subscripts are unparsable.
     * 
     * See also Ruby Rack's implementation:
     * http://github.com/chneukirchen/rack/blob/master/lib/rack/utils.rb#L69
     */
    def readRequestParams(req) {
        def params = [:]
        req.parameterNames.each { p ->
            def values = req.getParameterValues(p)
            values.each {value ->
                storeParam(subscripts(p), value, params)
            }
        }
        params
    }
	
    void readRequestRawParams(req) {
        def params = [:]
        req.parameterNames.each { p ->
            def values = req.getParameterValues(p)
            if (values.length == 1)
                values = values[0]
            params[p] = values
        }
        params
    }
	
    def storeParam(subscripts, value, params) {
        // Subscript "" means the param name contained [], array syntax.
        if (subscripts.size() == 0) {
            throw new RuntimeException("Malformed request params")
        } else if (subscripts.size() == 1) {
            def subscript = subscripts[0]
            if (subscript == "") {
                params << value
            } else {
                params[subscript] = value
            }
        } else {
            def subscript = subscripts[0]
            def nextSubscript = subscripts[1]
            if (subscript == "" && nextSubscript == "") {
                throw new RuntimeException("Malformed request params")
            }
            def empty = [:]
            if (nextSubscript == "") {
                empty = []
            }
            if (subscript == "") {
                if (!params instanceof List) {
                    throw new RuntimeException("Malformed request params")
                }
                if (params.isEmpty() || params[-1].containsKey(nextSubscript)) {
                    params << empty
                } 
                storeParam(subscripts[1..-1],value,params[-1])
            } else {
                if (!params instanceof Map) {
                    throw new RuntimeException("Malformed request params")
                }
                params[subscript] = params[subscript] ?: empty
                storeParam(subscripts[1..-1],value,params[subscript])
            }
            
        }
    }
	
    def subscripts(string) {
        def subscripts = []
        def symbol = ""
        int nesting = 0
        def wellFormed = true;
        string.each {c ->
            switch(c) {
                case '[':
                    if (++nesting != 1) {
                        wellFormed = false;
                    }
                    if (symbol != "") {
                        subscripts << symbol
                        symbol = ""	
                    }
                    break;
                case  ']':
                    if (--nesting != 0) {
                        wellFormed = false;
                    }
                    subscripts << symbol
                    symbol = ""
                    break;
                default:
                    symbol += c	
            }
        }
        wellFormed &= nesting == 0
        if (symbol != "") {
            subscripts << symbol
        }
        // TODO: consider throwing exception if !wellFormed 
        wellFormed ? subscripts : [string]
    }
	
}
