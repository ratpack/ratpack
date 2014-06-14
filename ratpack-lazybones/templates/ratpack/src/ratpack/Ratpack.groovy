import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack
import groovy.util.logging.Slf4j

@Slf4j
ratpack {
  handlers {
    get {
      render groovyTemplate("index.html", title: "My Ratpack App")
      log.debug "your debug info"
    }
        
    assets "public"
  }
}
