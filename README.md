Ratpack
=======

A micro web framework for Groovy
--------------------------------

Ratpack is inspired by the excellent [Sinatra][] framework for Ruby, and aims to make Groovy web development more classy.

  [Sinatra]: http://www.sinatrarb.com/


Requirements
------------

Groovy 1.7.1+ and Gradle (to build and fetch other dependencies).

Getting Started
---------------

Ratpack is still *very* beta. But, you can start using it right now.

To easily run your app from the command line, build the Ratpack project and add the binary to your PATH:

    gradle buildDistro
    export PATH=$PATH:`pwd`/build/ratpack/bin

Here's a basic "Hello, World" app:

    get("/") {
    	"Hello, World!"
    }

If you save the above code in `hello.groovy` and run it on the command line, it will start your app in Jetty on port 5000:

    $ ratpack hello.groovy 
    Starting Ratpack app with config:
    [port:5000]
    2011-05-28 07:44:51.408:INFO::Logging to STDERR via org.mortbay.log.StdErrLog
    2011-05-28 07:44:51.573:INFO::jetty-6.1.24
    2011-05-28 07:44:52.169:INFO::Started SocketConnector@0.0.0.0:5000
    ...

You can also use the 'runapp.groovy' script to auto restart your app when there are changes in the directory.

    $ groovy scripts/runapp.groovy appdir/hello.groovy appdir

POST and Other Verbs
--------------------

    post("/submit") {
        // handle form submission here
    }

    put("/some-resource") {
        // create the resource
    }

    delete("/some-resource") {
        // delete the resource
    }

    register("propfind", "/some-resource") {
        // you can register your own verbs
    }

    register(["get", "post"], "/formpage") {
        // you can register multiple verbs to the same handler
    }


URL Parameters
--------------

You can capture parts of the URL to use in your handler code using the colon character.
Any parameters that are captured are stored in the `urlparams` map.

    get("/person/:personid") {
        "This is the page for person ${urlparams.personid}"
    }

    get("/company/:companyname/invoice/:invoiceid") {
        def company = CompanyDAO.getByName(urlparams.companyname)
        def invoice = company.getInvoice(urlparams.invoiceid)
        // you get the idea
    }


GET and POST Parameters
-----------------------

Parameters in the query string or passed in via a `POST` request are available in the `params` map.

    get("/search") {
        def results = SearchEngine.search(params.q)
        // etc.
    }


Templates
---------

Render templates using the `render` method.
To specifiy where to load template files from, set the `templateRoot` setting.
If the file isn't found in the template root, the renderer will try to load it as a resource from the classpath.

    set 'templateRoot', 'myapp/templates'

    get("/") {
        render "homepage.html"
    }

You can also pass in a map to use in the template.

    get("/page/:pagename") {
        render "page.html", [name: urlparams.pagename]
    }

The template syntax is the same as Groovy's [SimpleTemplateEngine][].

  [SimpleTemplateEngine]: http://groovy.codehaus.org/Groovy+Templates


The Development Server
----------------------

The default port is 5000, but you can specify another if you wish by adding the following to your app:

    set 'port', 8080
