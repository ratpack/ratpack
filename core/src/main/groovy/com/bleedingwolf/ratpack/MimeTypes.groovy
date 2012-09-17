package com.bleedingwolf.ratpack

// Augments the list of MIME constants to include application/json (not sure why it doesn't already)
class MimeTypes extends org.mortbay.jetty.MimeTypes {
    public final static String APPLICATION_JSON = "application/json"
}
