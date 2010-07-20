package ratpack

import java.util.regex.Pattern

import ratpack.routing.Route;
import ratpack.routing.RoutingTable;

class RatpackApp {
	
	def handlers = [
		'GET': new RoutingTable(),
		'POST': new RoutingTable(),
	]
	
	def config = [:]
	
	void set(setting, value) {
		config[setting] = value
	}
	   
	void register(List methods, path, handler) {
		methods.each {
			register(it, path, handler)
		}
	}
	
	void register(method, path, handler) {
		method = method.toUpperCase()

		if(path instanceof String) {
			path = new Route(path)
		}
		
		def routingTable = handlers[method]
		if(routingTable == null) {
			routingTable = new RoutingTable()
			handlers[method] = routingTable
		}
		routingTable.attachRoute path, handler
	}
	
	Closure getHandler(method, subject) {
		return handlers[method.toUpperCase()].route(subject)
	}
	
	void get(path, handler) {
		register('GET', path, handler)
	}
	
	void post(path, handler) {
		register('POST', path, handler)
	}
	
	void put(path, handler) {
		register('PUT', path, handler)
	}
	
	void delete(path, handler) {
		register('DELETE', path, handler)
	}

}
