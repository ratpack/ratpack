# Ratpack

[![Build Status](https://drone.io/github.com/ratpack/ratpack/status.png)](https://drone.io/github.com/ratpack/ratpack/latest)

## A Micro Web Framework for Java (and Groovy)

Ratpack is micro web framework inspired by [Ruby's Sinatra](http://www.sinatrarb.com/). It is implemented in pure Java on top of [Netty](http://netty.io/ "Netty: Home"), but has specific support for [Groovy](http://groovy.codehaus.org/) and [Gradle](http://www.gradle.org/). It is not a J2EE solution. 

**Note:** Older (pre 0.7, non compatible) versions of Ratpack can be found [here](https://github.com/bleedingwolf/ratpack).

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

If you're interested in getting involved, please take a look at some of the [outstanding issues](https://github.com/ratpack/ratpack/issues) and comment away.

### Examples 

* [A standalone Groovy script app](https://github.com/ratpack/example-ratpack-standalone-groovy-script)
* [A Groovy app built with Gradle](https://github.com/ratpack/example-ratpack-gradle-groovy-app)
* [A Java app built with Gradle](https://github.com/ratpack/example-ratpack-gradle-java-app)