package com.bleedingwolf.ratpack.routing

import org.junit.Test

import com.bleedingwolf.ratpack.routing.Route
import com.bleedingwolf.ratpack.routing.RoutingTable
import static org.junit.Assert.*

class RoutingTableTest {

    @Test
    void singleStaticRoute() {
        def homepage = { "Homepage" }
        
        def table = new RoutingTable()
        table.attachRoute new Route("/"), homepage
        
        assertSame table.route("/").call(), "Homepage" 
    }
    
    @Test
    void multipleStaticRoutes() {
        def homepage = { "Homepage" }
        def aboutpage = { "About" }
        
        def table = new RoutingTable()
        table.attachRoute new Route("/"), homepage
        table.attachRoute new Route("/about"), aboutpage
        
        assertSame table.route("/about").call(), "About"
    }
    
    @Test
    void singleDynamicRoute() {
        def productpage = { "Product" }
        
        def table = new RoutingTable()
        table.attachRoute new Route("/product/:product"), productpage
        
        assertSame table.route("/product/9876").call(), "Product"
    }
    
    @Test
    void multipleDynamicAndStaticRoutes() {
        def homepage = { "Homepage" }
        def aboutpage = { "About" }
        def productpage = { "Product" }
        def profilepage = { "Profile" }
        
        def table = new RoutingTable()
        table.attachRoute new Route("/"), homepage
        table.attachRoute new Route("/about"), aboutpage
        table.attachRoute new Route("/product/:product"), productpage
        table.attachRoute new Route("/user/:user/profile"), profilepage

        assertSame table.route("/user/43/profile").call(), "Profile"
        assertSame table.route("/product/77").call(), "Product"
    }
    
    @Test
    void routingTableCurriesHandlerFunctions() {
        def handler = { params -> return "Order ${params.order} for customer ${params.customer}".toString() }
        
        def table = new RoutingTable()
        table.attachRoute new Route("/customer/:customer/order/:order"), handler
        
        assertEquals table.route("/customer/fred/order/123").call(), "Order 123 for customer fred"
    }
    
    @Test
    void routesAreMatchedInTheOrderTheyAreDefined() {
        def table = new RoutingTable()
        table.attachRoute new Route("/test"), { "Route 1" }
        table.attachRoute new Route("/test"), { "Route 2" }
        
        assertEquals table.route("/test").call(), "Route 1"
    }
}
