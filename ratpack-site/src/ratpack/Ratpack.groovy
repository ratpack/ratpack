import ratpack.error.ClientErrorHandler
import ratpack.groovy.templating.TemplatingModule
import ratpack.site.RatpackVersions
import ratpack.site.SiteErrorHandler
import ratpack.site.VersionsModule

import static ratpack.groovy.Groovy.*

ratpack {
  modules {
    register new VersionsModule(getClass().classLoader)
    bind ClientErrorHandler, new SiteErrorHandler()

    get(TemplatingModule).staticallyCompile = true
  }

  handlers { RatpackVersions versions ->
  	handler {
      if (request.headers.get("host").endsWith("ratpack-framework.org")) {
        redirect 301, "http://www.ratpack.io"
      }

      if (request.path.empty || request.path == "index.html") {
        response.headers.set "X-UA-Compatible", "IE=edge,chrome=1"
      }
      next()
    }

    get("index.html") {
      redirect 301, "/"
    }

    get {
      render groovyTemplate("index.html")
    }

    prefix("manual/snapshot") {
      assets "public/manual/$versions.snapshot"
    }

    prefix("manual/current") {
      assets "public/manual/$versions.current"
    }

    assets "public"
  }
}
