import ratpack.codahale.CodaHaleModule
import ratpack.error.ClientErrorHandler
import ratpack.groovy.templating.TemplatingModule
import ratpack.site.RatpackVersions
import ratpack.site.SiteErrorHandler
import ratpack.site.VersionsModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  modules {
    register new CodaHaleModule().healthChecks()
    register new VersionsModule(getClass().classLoader)
    bind ClientErrorHandler, new SiteErrorHandler()

    get(TemplatingModule).staticallyCompile = true
  }

  handlers { RatpackVersions versions ->
    handler {
      if (request.headers.get("host").endsWith("ratpack-framework.org")) {
        redirect 301, "http://www.ratpack.io"
        return
      }

      if (request.path.empty || request.path == "index.html") {
        response.headers.set "X-UA-Compatible", "IE=edge,chrome=1"
      }
      next()
    }

    prefix("assets") {
      handler {
        response.headers.add("Cache-Control", "max-age=86400, public") // cache for one day
        next()
      }
      assets "assets"
    }

    // The generated CSS has links to /images, remap
    // https://github.com/robfletcher/gradle-compass/issues/12
    prefix("images") {
      handler {
        response.headers.add("Cache-Control", "max-age=86400, public") // cache for one day
        next()
      }
      assets "assets/images"
    }

    get("index.html") {
      redirect 301, "/"
    }

    get {
      render groovyTemplate("index.html")
    }

    prefix("manual") {
      prefix("snapshot") {
        assets "manual/$versions.snapshot"
      }

      prefix("current") {
        assets "manual/$versions.current"
      }

      assets "manual"
    }

    assets "public"
  }
}
