import static org.ratpackframework.groovy.RatpackScript.ratpack
import static org.ratpackframework.groovy.Template.groovyTemplate

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