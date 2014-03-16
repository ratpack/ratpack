# Heroku

[Heroku][] is a polyglot cloud application platform.  It allows you to focus on writing applications in the language of your choice, and then easily deploy them to the cloud without having to manually manage servers, load balancing, log aggregation, etc.

The `ratpack-heroku` [Gradle][] plugin provides an easy way to run a Ratpack application on Heroku.

## Usage

Install the plugin in your build file.  You will likely want to use it in conjunction with the `ratpack` or `ratpack-groovy` plugins.

For example:

```language-groovy
buildscript {
    repositories {
        jcenter()
        maven { url "http://oss.jfrog.org/repo" } // For Ratpack snapshots
    }
    dependencies {
        classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
    }
}

apply plugin: "ratpack-groovy"
apply plugin: "ratpack-heroku"
```

Now, run the Gradle `generateBuildpackFiles` task to generate the files needed to run the application on Heroku.

```language-bash
./gradlew generateBuildpackFiles
```

Add the generated files to [Git][].  Together, they make up an [inline buildpack](https://github.com/kr/heroku-buildpack-inline).

You're now ready to tell Heroku how to build your application.  Use one of the following commands.

At application creation:

```language-bash
heroku create myapp --buildpack https://github.com/kr/heroku-buildpack-inline
```

After application creation:

```language-bash
heroku config:set BUILDPACK_URL=https://github.com/kr/heroku-buildpack-inline
```

When you next push your application to Heroku, it will be built using the [buildpack](https://devcenter.heroku.com/articles/buildpacks) embedded within the application repository.
