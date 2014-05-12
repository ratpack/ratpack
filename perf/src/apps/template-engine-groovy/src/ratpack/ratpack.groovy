import ratpack.groovy.templating.TemplatingModule
import ratpack.handlebars.HandlebarsModule
import ratpack.perf.incl.*
import ratpack.thymeleaf.ThymeleafModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack
import static ratpack.handlebars.Template.handlebarsTemplate
import static ratpack.thymeleaf.Template.thymeleafTemplate

ratpack {
  <% if (patch < 5) { %>
    modules {
      get(TemplatingModule).staticallyCompile = true
      register new HandlebarsModule()
      register new ThymeleafModule()
    }
  <% } else { %>
    bindings {
      config(TemplatingModule).staticallyCompile = true
      add new HandlebarsModule()
      add new ThymeleafModule()
    }
  <% } %>

  handlers {
    handler("stop", new StopHandler())

    handler("groovy-template") {
      render groovyTemplate("index.html", message: "Hello World!")
    }

    handler("handlebars") {
      render handlebarsTemplate('index.html', message: "Hello World!")
    }

    handler("thymeleaf") {
      render thymeleafTemplate('index', message: "Hello World!")
    }
  }
}
