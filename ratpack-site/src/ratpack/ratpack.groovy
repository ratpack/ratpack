import static org.ratpackframework.groovy.RatpackScript.ratpack

ratpack {
	handlers {
        get {
            response.sendFile("text/html", file('public/index.html'))
        }
        assets "public"
	}
}