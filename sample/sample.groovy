 
import ratpack.Ratpack
import ratpack.RatpackServlet

def app = Ratpack.app {
    
    get("/") {
        def ua = headers['user-agent']
        "Your user-agent: ${ua}"
    }
    
    get("/foo/:name") { params ->
        "Hello, ${params.name}"
    }
    
    get("/person/:id") { params ->
        "Person #${params.id}"
    }

}

RatpackServlet.serve(app)
