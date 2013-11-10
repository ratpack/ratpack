import static ratpack.groovy.Groovy.*

ratpack {
  handlers {
    assets "public"
    get("large-template") {
      render groovyTemplate("large.html")
    }
    get("text") {
      response.send "some plain string"
    }
  }
}