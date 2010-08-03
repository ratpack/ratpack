package com.bleedingwolf.ratpack;


public class RatpackRequestParamReader {
	def final SIMPLE_ARRAY_SUFFIX = /\[\]$/
	// TODO: handle maps, nesting, etc...
	// Ruby rack has a decent implementation:
	// http://github.com/chneukirchen/rack/blob/master/lib/rack/utils.rb#L69
    void readRequestParams(req, params) {
        req.parameterNames.each { p ->
            def values = req.getParameterValues(p)
			if (p =~ SIMPLE_ARRAY_SUFFIX) {
				def sanitizedP = (p =~ SIMPLE_ARRAY_SUFFIX).replaceFirst("")
				params[sanitizedP] = values
			} else {
				if (values.length == 1) {
					values = values[0]
				}
				params[p] = values
			}
        }
    }
	// TODO: Enable this version via a config option, 
	// in case user wants raw params.
	void readRequestParamsRaw(req, params) {
		req.parameterNames.each { p ->
			def values = req.getParameterValues(p)
			if (values.length == 1)
				values = values[0]
			params[p] = values
		}
	}
}
