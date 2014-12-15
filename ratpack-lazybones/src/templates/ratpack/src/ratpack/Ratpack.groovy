import ratpack.groovy.template.TextTemplateModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  bindings {
    add TextTemplateModule
  }

  handlers {
    get {
      render groovyTemplate("index.html", title: "My Ratpack App")
    }
        
    assets "public"
  }
}
