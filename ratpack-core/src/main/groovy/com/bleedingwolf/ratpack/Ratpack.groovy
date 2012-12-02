package com.bleedingwolf.ratpack

class Ratpack {
    
    static def app(closure) {
        def theApp = new RatpackApp()
        closure.delegate = theApp
        closure.call()
        return theApp
    }

}
