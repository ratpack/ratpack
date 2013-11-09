import static ratpack.groovy.RatpackScript.ratpack
import static ratpack.groovy.Template.groovyTemplate

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