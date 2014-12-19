<% if (patch > 11) { %>
import ratpack.groovy.template.MarkupTemplateModule
import ratpack.groovy.template.TextTemplateModule
<% } else { %>
import ratpack.groovy.markuptemplates.MarkupTemplatingModule
import ratpack.groovy.templating.TemplatingModule
<% } %>

import ratpack.handlebars.HandlebarsModule
import ratpack.perf.incl.*
import ratpack.thymeleaf.ThymeleafModule

import static ratpack.groovy.Groovy.*
import static ratpack.handlebars.Template.handlebarsTemplate
import static ratpack.thymeleaf.Template.thymeleafTemplate

ratpack {
  bindings {
    <% def c = patch > 11 ? "TextTemplateModule" : "TemplatingModule"  %>
    add($c) { ${c}.Config config -> config.staticallyCompile = true }
    add new HandlebarsModule()
    add new ThymeleafModule()
    add new <%= patch > 11 ? "MarkupTemplateModule" : "MarkupTemplatingModule" %>()
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
