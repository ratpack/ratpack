import org.ratpackframework.site.RatpackVersions
import org.ratpackframework.site.VersionsModule

import static org.ratpackframework.groovy.RatpackScript.ratpack

def indexPages = ["index.html"] as String[]

ratpack {
  modules {
    register new VersionsModule(getClass().classLoader, "versions.properties")
  }
  handlers { RatpackVersions versions ->
  	handler {
      println request.path
      if (request.path.empty || request.path == "index.html") {
        response.addHeader "X-UA-Compatible", "IE=edge,chrome=1"
      }
      next()
    }

    prefix("manual/snapshot") {
      println versions.snapshot
      assets "public/manual/$versions.snapshot", indexPages
    }

    prefix("manual/current") {
      assets "public/manual/$versions.current", indexPages
    }

    assets "public", indexPages
  }
}
