package ratpack.routing


class RoutingTable {

	def handlers = [:]
	
	def attachRoute(route, handler) {
		handlers[route] = handler
	}
	
	def route(subject) {
		def foundHandler = null
		handlers.keySet().each { route ->
			def params = route.match(subject)
			if(params != null) {
				def originalHandler = handlers[route]
				foundHandler = { ->
					originalHandler.delegate = delegate
					originalHandler(params)
				}
			}
		}
		return foundHandler
	}
	
}
