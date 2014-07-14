# Setup

Ratpack is purely a runtime.
There is no installable package.
To build Ratpack applications, you can use any JVM build tool.
The Ratpack project provides specific support for [Gradle](http://www.gradle.org) through plugins, but any could be used.

Ratpack is published as a set of library JARs.
The `ratpack-core` library is the only strictly required library.
Others such as `ratpack-groovy`, `ratpack-guice`, `ratpack-jackson`, `ratpack-test` etc. are optional.

With Ratpack on the classpath, you can use the API described in the next chapter to launch the application.

## Using the Gradle plugin(s)

TODO

## Using Lazybones project templates

[Lazybones](https://github.com/pledbrook/lazybones) is a command line tool that allows you to generate a project structure for any framework based on pre-defined templates.

Ratpack's Lazybones templates can be found on [Bintray](https://bintray.com) in the [ratpack/lazybones repository](https://bintray.com/ratpack/lazybones).
Templates are published with each Ratpack release and template versions are aligned with Ratpack release versions.
There are different types of Ratpack templates available, listed in the repository.
See the description of each for details.

See the [Lazybones documentation](https://github.com/pledbrook/lazybones#running-it) for help with installing Lazybones.

Lazybones commands are in the format...

```language-bash
lazybones create <ratpack template> <ratpack version> <app name>
```

With Lazybones installed, creating a new Ratpack application is as easy as…

```language-bash
lazybones create ratpack my-ratpack-app
cd my-ratpack-app
./gradlew run
```

This will use the latest available version of Ratpack.
If a specific version is required…

```language-bash
lazybones create ratpack x.x.x my-ratpack-app
cd my-ratpack-app
./gradlew run
```

Where `x.x.x` is a valid template version.  See the [Bintray template repository](https://bintray.com/ratpack/lazybones/ratpack-template/view) for all available template versions.
