import ratpack.groovy.templating.TemplatingModule
import ratpack.handlebars.HandlebarsModule
import ratpack.perf.incl.*
import ratpack.thymeleaf.ThymeleafModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack
import static ratpack.handlebars.Template.handlebarsTemplate
import static ratpack.thymeleaf.Template.thymeleafTemplate

ratpack {
  bindings {
    config(TemplatingModule).staticallyCompile = true
    add new HandlebarsModule()
    add new ThymeleafModule()
  }

  handlers {
    handler("stop", new StopHandler())

    def endpoint = System.getProperty("endpoint")

    if (endpoint == "groovy-template") {
      handler("groovy-template") {
        render groovyTemplate("index.html", message: "Hello World!")
      }
    }

    if (endpoint == "handlebars") {
      handler("handlebars") {
        render handlebarsTemplate('index.html', message: "Hello World!")
      }
    }

    if (endpoint == "thymeleaf") {
      handler("thymeleaf") {
        render thymeleafTemplate('index', message: "Hello World!")
      }
    }
  }
}
