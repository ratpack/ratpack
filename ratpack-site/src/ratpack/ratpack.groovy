import static org.ratpackframework.groovy.RatpackScript.ratpack

def versionsFile = getClass().classLoader.getResource("versions.properties")
def versions = new Properties()
versionsFile.openStream().withStream {
  versions.load(it)
}

def snapshotVersion = versions.snapshot
def currentVersion = versions.current
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
