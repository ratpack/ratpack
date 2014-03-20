# Groovy

[Groovy](http://www.groovy-lang.org/) is an alternative JVM programming language.
It has a strong synergy with Java and many language and library features that make it a compelling programming environment.
Ratpack provides strong integration with Groovy via the `ratpack-groovy` and `ratpack-groovy-test` libraries.
Writing Ratpack applications in Groovy generally leads to less code through Groovy's concise syntax compared to Java and a more productive and enjoyable development experience.
To be clear though, Ratpack applications do not _need_ to be written in Groovy.

Groovy is commonly known as a dynamic language.
However, Groovy 2.0 added full static typing and static compilation as an option.
Ratpack's Groovy support is strictly designed to fully support “static Groovy” and also leverages the newest features of Groovy to avoid introducing boilerplate code to achieve this goal.
Said another way, Ratpack's Groovy support does not use any dynamic language features and has a strongly typed API.

> TODO: find decent links describing static Groovy and use above

## Prerequisites

If you are new to Groovy, you may want to research the following foundational Groovy topics before proceeding:

1. Closures
1. The `def` keyword

> TODO: what else should be in this list? Also, links needed
>
> something else

## Ratpack Groovy API

> TODO: explain that Groovy API wraps Java API and mirrors it in corresponding ratpack.groovy.*

### @DelegatesTo

> TODO: explain this Groovy feature and outline Ratpack's extensive use of it

## GroovyRatpackMain

The `ratpack-groovy` library provides the [`GroovyRatpackMain`](api/ratpack/groovy/launch/GroovyRatpackMain.html) application entry point that bootstraps the Groovy support.
This extends from [`RatpackMain`](launching.html#ratpackmain), but forces the [handler factory](api/ratpack/launch/LaunchConfig.html\#getHandlerFactory\(\)) to be an instance of
[GroovyScriptFileHandlerFactory](api/ratpack/groovy/launch/GroovyScriptFileHandlerFactory.html).

## ratpack.groovy script

> TODO: introduce DSL used in this file, discuss reloading when in reloadable mode

## handlers {} DSL

> TODO: introduce the `GroovyChain` DSL, and closures as handlers

## Testing

> TODO: Discuss Groovy testing specifics (might move some content from the testing chapter)


