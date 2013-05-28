import static org.ratpackframework.groovy.RatpackScript.ratpack

ratpack {
  handlers {
  	handler {
      if (request.path.empty || request.path == "index.html") {
        response.addHeader "X-UA-Compatible", "IE=edge,chrome=1"
      }
      next()
    }
    assets "public", "index.html"
  }
}
