# Ratpack

## A Micro Web Framework for Groovy

Ratpack is inspired by the excellent [Sinatra](http://www.sinatrarb.com/) framework for Ruby, and aims to make Groovy web development more classy.

This project is the Ratpack Gradle Plugin. It depends on [Ratpack Core](http://github.com/tlberglund/ratpack-core), which contains all of the actual framework code. The Gradle plugin makes it easy to apply all of the Ratpack build and development functionality to a Ratpack project with a few lines of Gradle configuration. For an example of this, see the [Ratpack Project Template](http://github.com/tlberglund/ratpack-template).

Ratpack is implemented as a Gradle plugin. Every Ratpack project is also a Gradle build. This is a fundamental design decision of the framework (and a good one!), not a limitation in the present implementation.

## Using the Ratpack Gradle Plugin

To turn your build into a Ratpack project, apply the plugin as follows:

```
   apply plugin: 'ratpack'
   
   buildscript {
     repositories {
       mavenCentral()
     }
     dependencies {
       classpath 'com.augusttechgroup:ratpack-plugin:0.5'
     }
   }
   
   dependencies {
     groovy 'org.codehaus.groovy:groovy:1.8.6'
   }
```
 
## Using Ratpack

For documentation on Ratpack itself, see the [Ratpack Project Template](http://github.com/tlberglund/ratpack-template).
