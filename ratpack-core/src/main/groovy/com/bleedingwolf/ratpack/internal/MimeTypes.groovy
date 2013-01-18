package com.bleedingwolf.ratpack.internal

import groovy.transform.CompileStatic

// Augments the list of MIME constants to include application/json (not sure why it doesn't already)
@CompileStatic
class MimeTypes extends org.mortbay.jetty.MimeTypes {
    public final static String APPLICATION_JSON = "application/json"
}
