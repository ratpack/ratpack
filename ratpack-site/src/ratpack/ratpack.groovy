import ratpack.site.RatpackVersions
import ratpack.site.VersionsModule

import static ratpack.groovy.Groovy.*

ratpack {
  modules {
    register new VersionsModule(getClass().classLoader)
  }
  handlers { RatpackVersions versions ->
  	handler {
      if (request.path.empty || request.path == "index.html") {
        response.headers.set "X-UA-Compatible", "IE=edge,chrome=1"
      }
      next()
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
