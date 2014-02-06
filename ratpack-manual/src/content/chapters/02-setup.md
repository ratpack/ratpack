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

[Lazybones](https://github.com/pledbrook/lazybones) is a command line tool that allows you to generate a project structure
for any framework based on pre-defined templates.

Ratpack's pre-defined templates can be found on [Bintray](https://bintray.com) in the following [repository](https://bintray.com/ratpack/lazybones).
Templates are published with each Ratpack release and template versions are aligned with Ratpack release versions.

The quickest way to get started with Lazybones is via [GVM](http://gvmtool.net/), the Groovy enVironment Manager.  So first of all we need to download
GVM using [curl](http://curl.haxx.se/).  Windows users can use [Cygwin](http://www.cygwin.com/) for this.

```language-bash
curl -s get.gvmtool.net | bash
```

We can then install Lazybones with the following command...

```language-bash
gvm install lazybones
```

Lazybones commands are in the format...

```language-bash
lazybones create <ratpack template> <ratpack version> <app name>
```

With Lazybones installed then getting started with Ratpack is as easy as...

```language-bash
lazybones create ratpack my-ratpack-app
cd my-ratpack-app
./gradlew run
```

If a specific Ratpack version is required...

```language-bash
lazybones create ratpack 0.9.0 my-ratpack-app
cd my-ratpack-app
./gradlew run
```

Custom templates can also be defined and published locally or to your own Bintray repository. For more details and further configuration options see the
Lazybones [README](https://github.com/pledbrook/lazybones/blob/master/README.md) on GitHub.

