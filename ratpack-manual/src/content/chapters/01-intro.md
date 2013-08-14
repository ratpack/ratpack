# Introduction

Ratpack is a toolkit for creating web applications that run on the JVM. 
It provides a framework in which to structure code that responds to HTTP requests. 
It is not a full stack framework so does not prescribe the use of any other particular framework for other web application concerns such as security, persistence, marshalling etc. There are however optional modules that integrate specific tools, and integrating a new tool or framework is typically very simple.

Ratpack applications are not Java Enterprise Edition applications. 
That is, they do no deploy to Java Application servers and are not based on the Servlet specification.
Ratpack builds on Netty, which is a high performance IO engine.
Application deployments are self contained JVM applications.

Ratpack is designed to make simple applications trivially simple to write, yet stay supportive and promote productivity as your application inevitably grows in complexity.

## Highlights

The following section explains some of the key features and characteristics of Ratpack.

### High performance

Ratpack is designed for asynchronous IO and is extremely light weight. 
This means better performance and lower resource overheads than Java Enterprise applications.

### Implemented in Java, optimized for Groovy

Ratpack is implemented in Java 7, and designed around the upcoming Java 8's support for Lambda expressions.
As such, it is naturally suited to the Groovy language and its Lambda expression like Closures.

There is also a specific Groovy module add-on for Ratpack that utilizes Groovy features such as scripts, templating and closure delegates.
This module uses the latest Groovy features to provide a clean, concise, closure based DSL that is fully type safe, statically compilable, and IDE friendly.

### Google Guice integration

The Google Guice add-on library provides support for strongly typed dependency injection and modularity.
It is not required to use Google Guice with Ratpack.
Integration with other similar technologies is possible.

### Development tooling

Ratpack itself is purely a runtime.
However, advanced integration is provided with the Gradle build tool and IntelliJ IDEA IDE.
This integration is purely a development/build time convenience and not a requirement for using Ratpack.
Ratpack applications are simple Java applications so can be built with any build tool or IDE.

## How to read the documentation

TBD.

## Project Status

Ratpack is a new project, but stands on the shoulders of well established technologies and is built by developers with substantial experience in web application frameworks and tooling. 
It is currently pre 1.0 but under very active development.

No API compatibility guarantees are provided before the 1.0 release, from which point on the API will be strictly semantically versioned.

While it is new, it has received many man hours of development time.
