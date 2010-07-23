 
import com.bleedingwolf.ratpack.Ratpack
import com.bleedingwolf.ratpack.RatpackServlet

def app = Ratpack.app {
    
    get("/") {
        def ua = headers['user-agent']
        "Your user-agent: ${ua}"
    }
    
    get("/foo/:name") {
        "Hello, ${urlparams.name}"
    }
    
    get("/person/:id") {
        "Person #${urlparams.id}"
    }

}

RatpackServlet.serve(app)
