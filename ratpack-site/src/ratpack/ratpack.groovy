import org.ratpackframework.site.RatpackVersions
import org.ratpackframework.site.VersionsModule

import static org.ratpackframework.groovy.RatpackScript.ratpack

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
