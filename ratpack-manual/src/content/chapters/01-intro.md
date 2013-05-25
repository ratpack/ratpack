# Introduction

Ratpack is a toolkit for creating JVM based web applications.
It uses a focused, minimalistic, approach instead of trying to provide solutions to all problems.
Ratpack focusses on the HTTP at the core of your web application, leaving you to integrate other technologies of your choosing for concerns such as caching, persistence etc.

It can be used to easily create applications that are very simple, as well as being suitable for large complex applications.

## Features at a glance

The following section explains some of the key features and characteristics of Ratpack.

### Natively asynchronous

Ratpack processes HTTP requests asynchronously natively.
Asynchronous processing allows higher throughput due to more efficient use of machine resources.
Because Ratpack's APIs are predicated on asynchronous processing, integrating asynchronous libraries into your application is free of ceremony.
It is not a requirement that you write asynchronous Ratpack code. A blocking, synchronous, style can be used as well.

Ratpack uses Netty as the IO engine.

### Simple, self contained, deployment

Ratpack applications are simple Java applications.
Ratpack is not based on the J2EE servlet API.
A Ratpack application deployment is typically your application JAR, dependencies, static assets and a shell script to start the application.

### Implemented in Java, optimized for Groovy

Ratpack is implemented in 100% Java. However, the API has a modern, semi-functional, design.
This makes it convenient to write Ratpack applications in newer JVM languages such as Groovy.

There is a specific Groovy module add on for Ratpack that utilizes Groovy features such as scripts, templating and closures.
This module uses the latest Groovy features to provide a clean, concise, closure based DSL that is fully type safe, statically compilable, and IDE friendly.

### Small, composable, API

Ratpack applications center around a single interface, [`Handler`](api/org/ratpackframework/handling/Handler.html).
The processing of a request is effectively the traversal of a connected graph of handlers.
This gives you ultimate control over how requests are processed (or routed) within your application.
For example, you can simply define your own processing logic based on HTTP headers or query string parameters.

Ratpack provides built in handlers for common routing strategies such as request path and/or method.

### Modularity via Google Guice

The Google Guice add on library provides convenient integration with Google Guice as a dependency injection mechanism for convenient modularity and increased testability.
The Guice Ratpack add on is separate from the core, which enables you to use another dependency injection mechanism should you choose.

### Gradle build time support

The Ratpack Gradle plugin makes managing building Ratpack applications easy by leveraging the power of the Gradle build tool.
Gradle's IDE support also means that developing Ratpack applications within an IDE does not require custom configuration or IDE plugins.
Gradle plugins that are designed for Java based (including Groovy) projects work seamlessly for Ratpack projects.

As Ratpack applications are just plain Java applications, they can be built by any build tool or process.

### Development time hot reloading

When using the Ratpack Groovy add on library, runtime reloading of the application is supported by automatically detecting changes to the central `ratpack.groovy` file.
This means that most application changes take effect immediately without restart.

When using the Ratpack Gradle support, runtime reloading of class changes is enhanced through integration with the SpringSource SpringLoaded tool.
The Ratpack Gradle support also provides specific integration with IntelliJ IDEA that makes developing Ratpack applications with IDEA very convenient and productive.