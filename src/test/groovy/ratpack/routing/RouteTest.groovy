package ratpack.routing

import org.junit.Test

import ratpack.routing.Route
import static org.junit.Assert.*

class RouteTest {

    @Test
    void urlWithNoParams() {
        def route = new Route("/homepage")      
        assertNotNull route.match("/homepage")
        assertNull route.match("/")
    }
    
    @Test
    void urlWithParamAtStart() {
        def route = new Route("/:page/edit/")
        assertEquals route.match("/homepage/edit/"), [page: "homepage"]
        assertEquals route.match("/about/edit/"), [page: "about"]
    }
    
    @Test
    void urlWithParamAtEnd() {
        def route = new Route("/user/:userid")
        assertEquals route.match("/user/1234"), [userid: "1234"]
        assertEquals route.match("/user/8765"), [userid: "8765"]
    }
    
    @Test
    void urlWithManyParams() {
        def route = new Route("/company/:company/invoice/:invoice")
        assertEquals route.match("/company/initech/invoice/8888"), [company: "initech", invoice: "8888"]
        assertEquals route.match("/company/blamo/invoice/4321"), [company: "blamo", invoice: "4321"]
    }
}
