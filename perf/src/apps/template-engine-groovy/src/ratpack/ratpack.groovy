import ratpack.groovy.templating.TemplatingModule
import ratpack.handlebars.HandlebarsModule
import ratpack.perf.incl.*
import ratpack.thymeleaf.ThymeleafModule

import static ratpack.groovy.Groovy.groovyTemplate

<% if (patch >= 7) { %>
import ratpack.groovy.markuptemplates.MarkupTemplatingModule
<% } %>
import static ratpack.groovy.Groovy.ratpack

<% if (patch >= 7) { %>
import static ratpack.groovy.Groovy.groovyMarkupTemplate
<% } %>
import static ratpack.handlebars.Template.handlebarsTemplate
import static ratpack.thymeleaf.Template.thymeleafTemplate

ratpack {
  bindings {
    config(TemplatingModule).staticallyCompile = true
    add new HandlebarsModule()
    add new ThymeleafModule()
    <% if (patch >= 7) { %>
      add new MarkupTemplatingModule()
    <% } %>
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
        <% if (patch >= 7) { %>
          render groovyMarkupTemplate('index.gtpl', message: "Hello World!")
        <% } else { %>
          render "ok"
        <% } %>
      }
    }
  }
}
