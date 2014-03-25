# Performance Regression Tests

The perf project compares the performance of the current code to the last release.

## Overview

`src/apps` contains different applications that can be tested.
Each app provides 1 or more endpoints that can be tested, defined in `endpoints.json`.
Each directory in `src/apps` will be copied to `build/apps` and a version created that builds against the last release and HEAD.
The `Harness` main class drives the process.

## Apps

Each app must be a Gradle based project, using the Ratpack Gradle integration.

### Ratpack version differences

The app files are Groovy SimpleTemplateEngine templates, to cater for API and functional differences between Ratpack versions being compared.
Variables are available in the template that define the version of Ratpack the app is being created for.
For example, to deal with a moved class between versions…

```language-groovy
<% if (patch > 4) { %>
import ratpack.somepackage.SomeType;
<% } else { %>
import ratpack.otherpackage.SomeType;
<% } %>
```

See `perf.gradle` for what variables are available.

### Common files

The files in `src/common` are copied into every app.
They are also template files.

### App requirements

Every app must define an endpoint at `/stop` that stops the app.
There is a `StopHandler` in the common files that is the implementation.

### Endpoints

Currently, `endpoints.json` must be an array of strings where each string is the path to an endpoint to test.

### TODO

1. Provide a way to easily run up an app for manually testing the functionality (probably a gradle task)
1. Test all endpoints before doing any data gathering (i.e. make sure all endpoints work before starting measuring)
1. Add 'head' commit id to reports
1. Collect some kind of memory statistics (e.g. memory after gc after cooldown) - at least to catch leaks, hopefully to catch peak usage regressions
1. Count/check errored requests (i.e. status 500)