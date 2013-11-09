# Getting started

To start play with Ratpack, you just need to have Java and Groovy installed. 

If you don't have the Java JDK and *JAVA_HOME* set, follow [this guide](http://docs.oracle.com/javase/7/docs/webnotes/install/).

The easiest way to install groovy is via the GVM tool. Open a terminal and type:

```language-bash
curl -s get.gvmtool.net | bash
gvm install groovy
```

## Your first Ratpack application

Open a text file and save it as *ratpack.groovy*.

```language-groovy
@GrabResolver("https://oss.jfrog.org/artifactory/repo") // (1)
@Grab("org.ratpack-framework:ratpack-groovy:0.9.0-SNAPSHOT") 

import static ratpack.groovy.RatpackScript.ratpack

ratpack { // (2)
    handlers { 
        get { 
            response.send "Hello World" // (3)
        }
    }
}
```

Open a terminal window and type:

```language-bash
groovy ratpack.groovy
```

The first time you run the script, it might take a little while as Groovy downloads the libraries that are needed to run Ratpack.

You should see the following output:

```
Aug 15, 2013 11:10:06 PM ratpack.server.internal.NettyRatpackService startUp
INFO: Ratpack started for http://localhost:5050
```

If you open a web browser and navigate to *http://localhost:5050*, you should see your Ratpack application available. The words *'Hello World'* should appear.

Let's break down the script a little bit. Leave the server running now.

1. this tells the groovy script to download the libraries needed for Ratpack from the JFrog repository.
2. The main application, it is composed of several different handlers and modules.
3. This is a *get* handler. Since it has no parameters, it will resolve to the root of our application. We are telling it to return the words *'Hello World'*.

### Adding another handler

Let's try adding another handler. Open the *ratpack.groovy* file and add a second handler:

```language-groovy groovy-ratpack
ratpack { 
    handlers { 
        get { 
            getResponse().send "Hello World"
        }
        get('echo') {             // our new handler
            getResponse().send "echo"
        }
    }
}
```

Save the file and go to *http://localhost:5050/echo*. You should see the words echo appear. Notice we did not have to restart the server. Ratpack is capable of reloading any changes that you make. This is thanks to the Spring Reloading Agent.

## Dynamic Urls

### Path Tokens

But this is the web. It should be exciting and fun. We should at least be able to specify dynamic parameters.

Let's change our echo handler so it returns what we entered:

```language-groovy groovy-handlers
handlers {
    get('echo/:message') {
        getResponse().send getPathTokens().message
    }
}
```

Save the file and navigate to *http://localhost:5050/echo/repeat*.

You should see the words *'repeat'* returned on the screen. In our handler, we have used the syntax `:value`. This tells Ratpack that whatever we enter in this path should be saved into the `pathTokens` variable.

Interestingly, if you type *http://localhost:5050/echo/is this real!* you should see the URL encoded version of this string returned to you *is%20this%20real!*.

### Optional Path Tokens

What if we want parts of our path to be optional? 

Let's change our echo server to optionally remove parts of my message. 

This can be done by adding a question mark at the end of the optional parameter. `:this?`

```language-groovy groovy-handlers
handlers {
    get('echo/:message/:trim?'){
        getResponse().send getPathTokens().message - getPathTokens().trim
    }
}
```

* Save and enter *http://localhost:5050/echo/unomas/mas*, you should see *'uno'*.
* Enter *http://localhost:5050/echo/unodos*, you should see *'unodos'*.

You can have as many optional path tokens as you want as long as they are not followed by a mandatory token. 

```language-groovy groovy-handlers
handlers {
    get('echo/:message/:trim?/:add?') {
        getResponse().send getPathTokens().message - getPathTokens().trim + ( getPathTokens().add?:'' )
    }
}
```

* This should return *'unotres'* for *http://localhost:5050/echo/unodos/dos/tres*.
* *'unodos'* for *http://localhost:5050/echo/unodos/tres*

### Query Parameters

You can also have query parameters via the `request.queryParams` property in your handlers.

In the following example, we capitalize a message if the `upper=true` query parameter is passed into our handler.

```language-groovy groovy-handlers
handlers {
    get('echo/:message') {
        String message = getPathTokens().message
        if (getRequest().queryParams.upper) {
            message = message.toUpperCase()
        }
        getResponse().send message
    }
}
```

* If you enter *http://localhost:5050/echo/uno*, you'll get *'uno'*.
* If you enter *http://localhost:5050/echo/uno?upper=false*, you'll get *'uno'*.
* If you enter *http://localhost:5050/echo/uno?upper=true*, you'll get *'UNO'*.

The Ratpack DSL is very robust. You are able to specify different methods, prefixes, headers and content-negotiation behaviour. This should be outlined in the user manual soon. 

## Static Files

Ratpack makes it very easy to serve static files from a directory. Let's try adding an image and getting Ratpack to serve this image.

* Create a folder called *images* at the same level as your *ratpack.groovy* script

* Add a file *myimage.png*.

Now, we can create a simple handler for this directory.

In your *ratpack.groovy* file, add the following lines at the end:

```language-groovy groovy-handlers
handlers {
    prefix('images') {
        assets "images"
    }
}
```

If you navigate to *http://localhost:5050/images/myimage.png*, you should see the image from that directory.

The assets hierarchy also mirrors your folder structure; Add a file under *images/css/uno.css* and you should be able to see this under *http://localhost:5050/images/css/uno.css*.

It does not have to be a direct match to a directory, you can change the path:

```language-groovy groovy-handlers
handlers {
    prefix('images/public/today') {
        assets "images"
    }
}
```

Now the image can be found at *http://localhost:5050/images/public/today/myimage.png*.

## Templates

So far, we have only been returning text in our application. But Ratpack comes with the ability use external templates.

Here is a very simple handler that uses a groovyTemplate:

```language-groovy
import static ratpack.groovy.RatpackScript.ratpack
import static ratpack.groovy.Template.groovyTemplate

ratpack {
    handlers {
        get {
            render groovyTemplate("index.html", title: "My Ratpack App", content: "Saul Goodman is your buddy") // (1)
        }
    }
}
```

Note I added an extra import for `static ratpack.groovy.Template.groovyTemplate`.

Let's take a look at the actual rendering function, it takes in a groovyTemplate followed by a map of values.

The template looks as follows:

```language-markup
<!doctype html>
<head>
  <title>${model.title}</title>
</head>
<body>
  ${model.content}
</body>
</html>
```

Ratpack will look for the template relative to place where the script is being run. So this template should be saved under *templates/index.html*.

When we run this, you should get an HTML page where all the values inside `${}` are replaced with the real values.

There is also an implementation of using Handlebars templates by Marcin Erdmann. Look at [the implementation](https://github.com/ratpack/ratpack/tree/master/ratpack-handlebars) for more details.


## Blocking Operations

One of the interesting features about Ratpack is the fact that calls are non-blocking by default. However, if you have things that may take a little bit longer but are necessary for your application, you can force them into blocking calls.

```language-groovy groovy-handlers
import static ratpack.groovy.Util.exec

interface DbService {
    // Uses blocking IO
    String getName(String personId)
}

handlers {
    get("name/:id") { DbService db ->
        exec getBlocking(),
            { db.getName(getPathTokens().id) },
            { getResponse().send("name is: $it") }
    }
}
```

The `exec` block takes two closures, the first one is the blocking operation, followed by what needs to happen on success.

You can also provide an optional failure condition to the exec blocking operation, as outlined by the following test:

```language-groovy groovy-handlers
import static ratpack.groovy.Util.exec

interface DbService {
    // Uses blocking IO
    String getName(String personId)

    void logError(Exception e)
}

handlers {
    get("name/:id") { DbService db ->
        exec getBlocking(),
            { db.getName(getPathTokens().id) },
            { Exception e -> db.logError(e); error(e) },
            { getResponse().send("name is: $it") }
    }
}
```

## Building a Java-based application with Gradle

While our small script is nice, it is fairly difficult to deploy and grow. We really need a good project structure to hold our application together.

Luckily, there is a tool called [LazyBones](https://github.com/pledbrook/lazybones) that is capable of generating a good Groovy / Gradle application template for us.

First, install Lazybones:

```language-bash
gvm install lazybones
```

Next, create your Ratpack project and provide it with a name:

```language-bash
lazybones create ratpack myApp
```

Bear in mind that there are two versions of the Ratpack module, *ratpack-lite* is a stripped down bare-bones template with little or no content.

You should now see an application created for you. To run the application, simply call:

```language-bash
./gradlew run
```

To create a deployable version of your application, call:

```language-bash
./gradlew install-app
```

Let's explore the generated structure:

```language-bash
├── README.md
├── build.gradle
├── gradle
├── gradlew
├── gradlew.bat
└── src
    ├── main
    │   └── groovy
    ├── ratpack
    │   ├── config.groovy
    │   ├── public
    │   │   ├── images
    │   │   ├── lib
    │   │   ├── scripts
    │   │   └── styles
    │   ├── ratpack.groovy
    │   └── templates
    │       └── index.html
    └── test
        └── groovy
```

In this template, you can put your tests under *src/test/groovy* and your main code under *src/main/groovy*.

There is a *config.groovy* file that adds additional parameters to your application, as well as a *ratpack.groovy* file that contains an example of templating and has static assets set up to serve from the *public* directory.

Overall, this template is very intuitive and I highly recommend using this structure instead of trying to forge your own bit of madness.

There is a section with GitHub repositories of other example applications if you would like to explore alternatives.

## Modularizing your application

As your application grows, it becomes more and more important to separate out bits of code so they can be tested and developed in isolation.

Ratpack provides a very powerful dependency injection mechanism powered by [Google Guice](https://code.google.com/p/google-guice/).

While there is a more direct mechanism for injecting dependencies, Ratpack also offers the ability to define dependencies via modules.

If you look at the [MongoDB Ratpack Angular project](https://github.com/tomaslin/ratpack-mongo-angular) there is a [MongoModule](https://github.com/tomaslin/ratpack-mongo-angular/blob/master/server/src/main/groovy/com/tomaslin/mongopack/MongoModule.groovy) class that injects a mongo service based on the provided configuration. Then in the [ratpack.groovy](https://github.com/tomaslin/ratpack-mongo-angular/blob/master/server/src/ratpack/ratpack.groovy) file, you will see that there is a reference to modules:

```language-groovy groovy-ratpack
// MongoModule
import com.google.inject.AbstractModule

class MongoService {
    List<String> getEntries() {
        // connect to db
    }
}

class MongoModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MongoService)                  // (2)
    }
}

// ratpack.groovy
ratpack {
    modules {
        register new MongoModule()
    }

    handlers { MongoService mongoService -> // (1)
        post("some/path") {
            getResponse().send mongoService.entries.join("\n")
        }
    }
}
```

In *(1)*, we are calling the injected service globally so it applies to the all the handlers.

The most important line here is *(2)*, where the actual mongo service is bound to the module context.

This is then available to *(1)* above and provides a nice simple way of adding additional functionality to our applications without polluting the *ratpack.groovy* server script too much.

### Per-handler dependency injection

Injected services can also be accessed on a per-handler basis instead of globally.

This looks as follows:

```language-groovy groovy-handlers
interface PeopleDAO {
    void save(String person)
}

handlers {
    post("api/person") { PeopleDAO dao ->
        dao.save(getRequest().text)
        getResponse().send "saved"
    }
}
```

Notice that the injected dependency is referenced at the handler level and not in the handlers closure.

## Deploying your application to the Cloud

* Marco Vermeulen has worked very hard in getting Ratpack deployed into Heroku. You can read his [blog post](http://wiredforcode.com/blog/2013/08/05/deploy-ratpack-on-heroku/) on how to deploy Ratpack using a buildpack. Marco is working on a [gradle plugin](https://github.com/marcoVermeulen/gradle-heroku) that will make deployment into Heroku fairly simple.

* If you would rather build your application locally instead of having Heroku do it, Tomas Lin has some instructions [on how to deploy to Cloud Foundry without a Buildpack](http://fbflex.wordpress.com/2013/08/07/ratpack-to-cloudfoundry-with-java-buildpack/).

## Ratpack Examples 

* [A standalone Groovy script app](https://github.com/ratpack/example-ratpack-standalone-groovy-script)
* [A Groovy app built with Gradle](https://github.com/ratpack/example-ratpack-gradle-groovy-app)
* [A Java app built with Gradle](https://github.com/ratpack/example-ratpack-gradle-java-app)

## Reference Projects

There are a few good projects you can look at for inspiration:

* [FOASS](https://github.com/danveloper/ratpack-foaas/)
* [Ratpack Website](https://github.com/ratpack/ratpack/tree/master/ratpack-site)
* [Mid Century Ipsum](https://github.com/robfletcher/midcentury-ipsum/)
* [GORM and MongoDB with Ratpack](https://github.com/tyama/ratpack-gorm-mongo-example/)
