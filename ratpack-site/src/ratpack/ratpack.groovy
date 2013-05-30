import static org.ratpackframework.groovy.RatpackScript.ratpack

def snapshotVersion = System.getProperty("snapshotVersion")
def currentVersion = System.getProperty("currentVersion")
def indexPages = ["index.html"] as String[]

ratpack {
  handlers {
  	handler {
      if (request.path.empty || request.path == "index.html") {
        response.addHeader "X-UA-Compatible", "IE=edge,chrome=1"
      }
      next()
    }

    path("manual/snapshot") {
      assets "public/manual/$snapshotVersion", indexPages
    }

    path("manual/current") {
      assets "public/manual/$currentVersion", indexPages
    }

    assets "public", indexPages
  }
}
