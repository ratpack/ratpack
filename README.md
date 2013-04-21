This is the future home of the source of www.ratpack-framework.com.

If you'd like to help build a Ratpack app as this site, then please see https://github.com/ratpack/ratpack/issues/4.

## Current status

### Running this application

In order to build and run this app you will need:

* [Node.js][1]
* [Ruby][2]
* [Compass][3]

Before running the project for the first time you will need to run `npm install` from the project directory in order to install the [Grunt][4] modules required.

To run the application in development mode use `gradle run` as normal for a Ratpack application. _Grunt_ tasks that compile the compass stylesheets are automatically executed by the Gradle build.

Currently the following pages are available:

* `/` redirects to `/index.html`.
* `/logo.html` - prototypes for the Ratpack logo.
* `/chapter1` - a markdown rendered template.

[1]:http://nodejs.org/
[2]:http://www.ruby-lang.org/en/downloads/
[3]:http://compass-style.org/install/
[4]:http://gruntjs.com/