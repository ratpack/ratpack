# Heroku

[Heroku][] is a polyglot cloud application platform.  It allows you to focus on writing applications in the language of your choice, and then easily deploy them to the cloud without having to manually
manage servers, load balancing, log aggregation, etc.

Ratpack applications can be deployed to Heroku easily using the default Gradle [buildpack](https://devcenter.heroku.com/articles/buildpacks).
To integrate with Heroku, we're going to declare that Heroku should call the `installApp` build target to install the application locally  and use the generated start script to run the application.

## settings.gradle

By default, Gradle uses the project's directory name as the project's name.  In Heroku (and some CI servers), the project is built in a temporary directory with a randomly assigned name.  To ensure
that the project uses a consistent name, add a declaration to `settings.gradle` in the root of your project (substituting your app name):

```language-groovy
rootProject.name = "my-ratpack-app"
```

## build.gradle

The [Gradle buildpack](https://github.com/heroku/heroku-buildpack-gradle) calls the `stage` target on your build, so define one that depends on `installApp`.

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

apply plugin: "ratpack"

task stage() {
  dependsOn installApp
}
```

## Procfile

Heroku uses [`Procfile`](https://devcenter.heroku.com/articles/procfile) to declare what commands are run by your application.  In this file, add a declaration that the `web` process type should run
your Ratpack application (substituting your app name).

```language-bash
web: build/install/my-ratpack-app/bin/my-ratpack-app
```

## system.properties

Specify your desired Java version in `system.properties`:

```language-java
java.runtime.version=1.7
```

## Application creation

The application is now ready for creation.  Commit any remaining files to [Git][], then run the following commands (substituting your app name):

```language-bash
heroku create my-ratpack-app
heroku config:set MY_RATPACK_APP_OPTS='-Dratpack.port=$PORT -Dratpack.publicAddress=http://my-ratpack-app.herokuapp.com'
git push heroku master:master
```
