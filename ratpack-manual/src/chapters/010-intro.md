# Introduction

Ratpack is micro web framework inspired by Ruby's Sinatra.
It is implemented in pure Java on top of Netty, but has specific support for Groovy and Gradle.
It is not a J2EE solution.

## Detail

    ratpack {
      routing {
        get("foo/bar") {
          response.send "Yeah!"
        }
      }
    }

### Features

* Minimalistic, light, API
* Self contained (no container necessary)
* Async IO at the networking layer via Netty (i.e. not a thread-per-connection model)
* Dependency injection via Google Guice
* Extensibility (entire stack composition is done through overridable Guice modules)
* Groovy DSL support (i.e. app in a script)
* Latest Groovy 2 features for improved IDE intellisense
* Development time hot reloading (via [SpringSource's SpringLoaded](https://github.com/SpringSource/spring-loaded))
* Gradle plugin for development and packaging support, including advanced IntelliJ IDEA support

### Project Status

Ratpack is currently at verison `0.7.0-SNAPSHOT`, which is the first version since the move to Netty (away from Jetty and J2EE).

As we near a `0.7.0` final release, the following are tasks that are outstanding that we'd love your help with:

1. Creating of a website at `http://ratpack-framework.org`
2. Construction of a user manual

### Examples

* https://github.com/ratpack/groovy-web-console
* https://github.com/ratpack/groovy-script-example