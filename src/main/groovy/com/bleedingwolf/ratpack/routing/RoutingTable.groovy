package com.bleedingwolf.ratpack.routing


class RoutingTable {

    def routeHandlers = []
    
    def attachRoute(route, handler) {
        routeHandlers << [route: route, handler: handler]
    }
    
    def route(subject) {
        def found = routeHandlers.find { null != it.route.match(subject) }
        if (found) {
            def params = found.route.match(subject)
            def foundHandler = { ->
                found.handler.delegate = delegate
                found.handler(params)
            }
            return foundHandler
        }
        return null
    }
}