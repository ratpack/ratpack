import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.error.ClientErrorHandler
import ratpack.groovy.templating.TemplatingModule
import ratpack.site.RatpackVersions
import ratpack.site.SiteErrorHandler
import ratpack.site.VersionsModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  modules {
    register new CodaHaleMetricsModule().healthChecks()
    register new VersionsModule(getClass().classLoader)
    bind ClientErrorHandler, new SiteErrorHandler()

    get(TemplatingModule).staticallyCompile = true
  }

  handlers { RatpackVersions versions ->

    def cacheFor = 60 // one hour

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
        response.headers.add("Cache-Control", "max-age=$cacheFor, public")
        next()
      }
      assets "assets"
    }

    // The generated CSS has links to /images, remap
    // https://github.com/robfletcher/gradle-compass/issues/12
    prefix("images") {
      handler {
        response.headers.add("Cache-Control", "max-age=$cacheFor, public")
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
      get {
        redirect 301, "manual/current/"
      }
      handler {
        response.headers.add("Cache-Control", "max-age=$cacheFor, public")
        next()
      }
      fileSystem("manual") {
        prefix("snapshot") {
          assets versions.snapshot
        }
        prefix("current") {
          assets versions.current
        }
        assets ""
      }
    }

    assets "public"
  }
}
