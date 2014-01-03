import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.groovy.templating.TemplatingModule
import ratpack.handling.Handlers
import ratpack.jackson.JacksonModule
import ratpack.path.PathBinding
import ratpack.site.GitHubApi
import ratpack.site.IssuesService
import ratpack.site.RatpackVersions
import ratpack.site.SiteModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  modules {
    register new JacksonModule()
    register new CodaHaleMetricsModule().metrics()
    register new SiteModule()
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

    prefix("versions") { RatpackVersions versions ->
      get {
        background {
          versions.all
        } then { RatpackVersions.All all ->
          render groovyTemplate("versions.html", versions: all)
        }
      }

      prefix(":version") { IssuesService issuesService ->
        get {
          background {
            versions.all
          } then { RatpackVersions.All all ->
            def version = all.find(allPathTokens.version)
            if (version == null) {
              clientError(404)
            } else {
              background {
                issuesService.closed(version)
              } then { IssuesService.IssueSet issues ->
                render groovyTemplate("version.html", version: version, issues: issues)
              }
            }
          }
        }
      }
    }

    prefix("manual") { RatpackVersions versions ->
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
            handler {
              def snapshot = get(PathBinding).boundTo == "snapshot"
              def version = snapshot ? versions.snapshot : versions.current
              if (version) {
                respond Handlers.assets(version.version, launchConfig.indexFiles)
              } else {
                clientError(404)
              }
            }
          }
        }
      }
    }

    assets "public"
  }
}
