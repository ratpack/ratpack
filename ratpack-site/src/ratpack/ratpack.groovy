import static org.ratpackframework.groovy.RatpackScript.ratpack

ratpack {
  handlers {
    assets "public", "index.html"
    path("forums") {
      get("user") {
        // TODO update this to the vanity url
        response.redirect "http://ratpack-users.19683.n7.nabble.com/"
      }
    }
  }
}