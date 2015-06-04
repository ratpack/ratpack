import ratpack.groovy.template.MarkupTemplateModule
import ratpack.groovy.template.TextTemplateModule
import ratpack.handlebars.HandlebarsModule
import ratpack.thymeleaf.ThymeleafModule

import static ratpack.groovy.Groovy.*
import static ratpack.handlebars.Template.handlebarsTemplate
import static ratpack.thymeleaf.Template.thymeleafTemplate

import ratpack.perf.incl.*

<% if (patch >= 14) { %>
  import ratpack.handling.ResponseTimer
<% } %>

ratpack {
  <% if (patch < 14) { %>
    serverConfig { it.timeResponses(true) }
  <% } %>
  bindings {
    <% if (patch >= 14) { %>
      bindInstance ResponseTimer.decorator()
    <% } %>
    module TextTemplateModule, { it.staticallyCompile = true }
    add HandlebarsModule
    add ThymeleafModule
    module MarkupTemplateModule
  }

  handlers {
    handler("stop", new StopHandler())

    def endpoint = System.getProperty("endpoint")

    if (endpoint == "groovy-template") {
      path("groovy-template") {
        render groovyTemplate("index.html", message: "Hello World!")
      }
    }

    if (endpoint == "handlebars") {
      path("handlebars") {
        render handlebarsTemplate('index.html', message: "Hello World!")
      }
    }

    if (endpoint == "thymeleaf") {
      path("thymeleaf") {
        render thymeleafTemplate('index', message: "Hello World!")
      }
    }

    if (endpoint == "groovy-markup") {
      path("groovy-markup") {
        render groovyMarkupTemplate('index.gtpl', message: "Hello World!")
      }
    }
  }
}
