# Deploying to Heroku

[Heroku][] is a scalable polyglot cloud application platform.
It allows you to focus on writing applications in the language of your choice, and then easily deploy them to the cloud without having to manually manage servers, load balancing, log aggregation, etc.
Heroku does not have, nor does it need, any special Ratpack support above its [generic support for JVM applications](http://java.heroku.com).
Heroku is a rich platform, with many [add-ons](https://addons.heroku.com) such as Postgres, Redis, Memcache, RabbitMQ, New Relic, etc.
It is a compelling option for serving Ratpack applications.

Deployments to Heroku are typically in source form.
Deploying is as simple as performing a Git push at the end of your CI pipeline.
Many popular cloud CI tools such as [drone.io](https://drone.io/) and [Travis-CI](https://travis-ci.org) (among others) have convenient support for pushing to Heroku.

It is recommended to read the [Heroku Quickstart](https://devcenter.heroku.com/articles/quickstart) and [Buildpack](https://devcenter.heroku.com/articles/buildpacks) documentation if you are new to Heroku.
The rest of this chapter outlines the requirements and necessary configuration for deploying Ratpack applications to Heroku.

## Gradle based builds

Ratpack applications can be built by any build system, but the Ratpack team recommends [Gradle](http://gradle.org).
Heroku has native support for Gradle via the [Gradle buildpack](https://devcenter.heroku.com/articles/buildpacks), which works well with the Ratpack Gradle plugins.

All Gradle projects should use the [Gradle Wrapper](http://www.gradle.org/docs/current/userguide/gradle_wrapper.html).
If the wrapper scripts are present in your project, Heroku will detect that your project is built with Gradle.

### Java Version

Heroku allows the required Java version to be set via `system.properties` file in the root of the source tree.

```
java.runtime.version=1.7
```

At the time of writing, this file is required as Heroku defaults to Java 6 and Ratpack requires Java 7.

### Building

The Gradle buildpack will invoke `./gradlew stage`.
The Ratpack Gradle plugins do not add a `stage` task to your build, so you need to add it yourself and make it build your application.
The simplest way to do this is make the `stage` task depend on the `installApp` task which _is_ added by the Ratpack Gradle plugins.

A minimalistic `build.gradle` looks like this:

```language-groovy gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
  }
}

apply plugin: "io.ratpack.ratpack-java"

task stage {
  dependsOn installApp
}
```

This will build your application and install it into the directory `build/install/«project name»`.

#### Setting the project name

By default, Gradle uses the project's directory name as the project's name.
In Heroku (and some CI servers), the project is built in a temporary directory with a randomly assigned name.
To ensure that the project uses a consistent name, add a declaration to `settings.gradle` in the root of your project:

```language-groovy
rootProject.name = "«project name»"
```
This is a good practice for any Gradle project.

### Running (Procfile)

The `Procfile` lives at the root of your application and specifies the command that Heroku should use to start your application.
In this file, add a declaration that Heroku should start a `web` process by executing the launch script created by the build.

```language-bash
web: build/install/«project name»/bin/«project name»
```

### Configuration

There are several ways to configure the environment for applications deployed to Heroku.
You may want to use these mechanisms to set environment variables and/or JVM system properties to configure your application.

The application entry points that are used when using the `ratpack` and `ratpack-groovy` Gradle plugins support using
JVM system properties to contribute to the [`LaunchConfig`](api/ratpack/launch/LaunchConfig.html) (see the [launching chapter](launching.html) chapter for more detail).
The starter scripts created by the Ratpack Gradle plugins, support the standard `JAVA_OPTS` environment variable and an app specific `«PROJECT_NAME»_OPTS` environment variable.
If your application name was `foo-Bar`, then the environment variable would be named `FOO_BAR_OPTS`.

One way to bring this all together is to launch your application via `env`:

```language-bash
web: env "FOO_BAR_OPTS=-Dratpack.other.dbPassword=secret" build/install/«project name»/bin/«project name»
```

It is generally preferable to not use `JAVA_OPTS` as Heroku sets this to [useful defaults](https://devcenter.heroku.com/articles/java-support#environment) for the platform.

Another approach is to use [config vars](https://devcenter.heroku.com/articles/config-vars).
The benefit of setting the environment via the Procfile is that this information is in your versioned source tree.
The benefit of using config vars is that they are only available to those with permissions to view/change them with Heroku.
It is possible to combine both approaches by setting config vars for values that should be secret (like passwords) and referencing them in your Procfile.

```language-bash
web: env "FOO_BAR_OPTS=-Dratpack.other.dbPassword=$SECRET_DB_PASSWORD" build/install/«project name»/bin/«project name»
```

Now it is easy to see which properties and environment variables are set in the source tree, but sensitive values are only visible via the Heroku management tools.

## Other build tools and binary deployments

The Ratpack project does not provide any “official” integration with other build tools.
However, it is quite possible to use whatever tool you like to build a Ratpack application for Heroku or even to deploy in binary form.

Once you have a compiled Ratpack application in the Heroku environment (either through building with another build tool or by binary deployment),
you can simply start the application by using `java` directly.

```language-bash
web: java ratpack.groovy.launch.GroovyRatpackMain
```

See the [launching chapter](launching.html) chapter for more detail on starting Ratpack applications.

## General Considerations

### Port and public address

You may want to consider setting `-Dratpack.publicAddress` to the public name of your application so that application redirects work as expected.
See [`redirect()`](api/ratpack/handling/Context.html#redirect-java.lang.String-) for more details.

Heroku assigns each application an ephemeral port number, made available by the `PORT` environment variable.
The [`RatpackMain`](api/ratpack/launch/RatpackMain.html) entry point implicitly supports this environment variable if the `ratpack.port` system property is not set.
