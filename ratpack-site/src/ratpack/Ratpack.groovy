import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.groovy.templating.TemplatingModule
import ratpack.handling.Handlers
import ratpack.jackson.JacksonModule
import ratpack.path.PathBinding
import ratpack.remote.RemoteControlModule
import ratpack.site.SiteModule
import ratpack.site.github.GitHubApi
import ratpack.site.github.GitHubData
import ratpack.site.github.RatpackVersion
import ratpack.site.github.RatpackVersions

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  modules {
    register new JacksonModule()
    register new CodaHaleMetricsModule().metrics()
    register new SiteModule(launchConfig)
    register new RemoteControlModule()
    get(TemplatingModule).staticallyCompile = true
  }

  handlers {

    def cacheFor = 60 * 10 // ten mins

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

    handler("reset") { GitHubApi gitHubApi ->
      byMethod {
        if (launchConfig.reloadable) {
          get {
            gitHubApi.invalidateCache()
            render "ok"
          }
        }
        post {
          gitHubApi.invalidateCache()
          render "ok"
        }
      }
    }

    prefix("versions") {
      get { RatpackVersions versions ->
        versions.all.subscribe { RatpackVersions.All all ->
          render groovyTemplate("versions.html", versions: all)
        }
      }

      prefix(":version") {
        get { RatpackVersions versions, GitHubData gitHubData ->
          versions.all.subscribe { RatpackVersions.All all ->
            def version = all.find(allPathTokens.version)
            if (version == null) {
              clientError(404)
            } else {
              gitHubData.closed(version).subscribe {
                render groovyTemplate("version.html", version: version, issues: it)
              }
            }
          }
        }
      }
    }

    prefix("manual") {
      fileSystem("manual") {
        get {
          redirect 301, "manual/current/"
        }

        handler {
          response.headers.add("Cache-Control", "max-age=$cacheFor, public")
          next()
        }

        assets ""

        for (label in ["snapshot", "current"]) {
          prefix(label) {
            handler { RatpackVersions versions ->

              def snapshot = get(PathBinding).boundTo == "snapshot"
              (snapshot ? versions.snapshot : versions.current).subscribe { RatpackVersion version ->
                if (version) {
                  respond Handlers.assets(launchConfig, version.version, launchConfig.indexFiles)
                } else {
                  clientError(404)
                }
              }
            }
          }
        }
      }
    }

    assets "public"
  }
}
