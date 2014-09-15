import ratpack.groovy.markuptemplates.MarkupTemplatingModule
import ratpack.groovy.templating.TemplatingModule
import ratpack.handlebars.HandlebarsModule
import ratpack.perf.incl.*
import ratpack.thymeleaf.ThymeleafModule

import static ratpack.groovy.Groovy.*
import static ratpack.handlebars.Template.handlebarsTemplate
import static ratpack.thymeleaf.Template.thymeleafTemplate

ratpack {
  bindings {
    config(TemplatingModule).staticallyCompile = true
    add new HandlebarsModule()
    add new ThymeleafModule()
    add new MarkupTemplatingModule()
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

    if (endpoint == "groovy-markup") {
      handler("groovy-markup") {
        render groovyMarkupTemplate('index.gtpl', message: "Hello World!")
      }
    }
  }
}
