import ratpack.groovy.templating.TemplatingModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  bindings {
    add TemplatingModule
  }

  handlers {
    get {
      render groovyTemplate("index.html", title: "My Ratpack App")
    }
        
    assets "public"
  }
}
