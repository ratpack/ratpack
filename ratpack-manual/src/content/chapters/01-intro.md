# Introduction

Ratpack is a toolkit for creating web applications that run on the JVM. 
It provides a structure in which to write code that responds to HTTP requests.
It is not a full stack framework so does not prescribe the use of any other particular framework for other web application concerns such as security, persistence, marshalling etc.
There are however optional add on libraries that integrate specific tools, and integrating a new tool or framework is typically very simple.

Ratpack applications are not Java Servlet applications.
They do not deploy to Java application servers.
Ratpack builds on Netty, which is a high performance IO engine.
Application deployments are self contained JVM applications.

Ratpack tries to strike the balance between provide helpful abstractions and infrastructure, while not being too prescriptive.
It aims to make it easy to write small applications, yet not get in the way as your application evolves and becomes more complex

## Highlights

Here are some key highlights:

* High throughput / Asynchronous IO
* Optimized for Java 8 & Groovy
* Dependency injection via Google Guice
* Development time support (reloading and build tooling)

Read on for details.

## How to read the documentation

The canonical reference documentation for Ratpack is the [Javadoc](api/).
This manual acts as a starting point for different aspects of Ratpack and as a mechanism to pull together larger ideas and patterns.
The manual frequently introduces topics at a high level and links through to the Javadoc for detailed API information.
Much work goes in to making the Javadoc (hopefully) useful.

## Project Status

Ratpack is a new project, but stands on the shoulders of well established technologies and is built by developers with substantial experience in web application frameworks and tooling. 
It is currently pre 1.0 but under very active development.

No API compatibility guarantees are provided before the 1.0 release, from which point on the API will be strictly semantically versioned.

While it is new, it has received many man hours of development time.
