# Introduction

Ratpack is a set of Java libraries that facilitate fast, efficient, evolvable and well tested HTTP applications.

It is built on the highly performant and efficient Netty event-driven networking engine.

Ratpack focuses on allowing HTTP applications to be efficient, modular, adaptive to new requirements and technologies, and well-tested over time.

## Compared to…

This section contrasts Ratpack with other similar types of technologies to help explain what Ratpack is and what it isn't.

> If you disagree with description given of a particular technology/framework below, please [edit this documentation on GitHub](https://github.com/ratpack/ratpack/blob/master/ratpack-manual/src/content/chapters/01-intro.md).
> Moreover, if you think that another comparison is warranted please request it.

### Netty

[http://netty.io](http://netty.io)

Ratpack is built on top of Netty.
Netty is a low level, extremely performant, general networking engine.
It is extensible, well documented and very stable.
It provides HTTP support out of the box, along with other common protocols, but importantly can also be used for custom protocols.
 
Netty is more performant than Ratpack, as it is at a lower level of abstraction.
It provides practically no support for structuring an _application_ beyond the networking protocol concerns.
That is, to build a non trivial HTTP application in pure Netty would require writing a lot of the kind of support Ratpack provides.
While Netty is more performant, Ratpack does not add _significant_ overhead.
Performance is a key concern for Ratpack.

Ratpack does not completely abstract over Netty.
You can access Netty through Ratpack's API and potentially use it directly.
This could be used to implement support for custom, non HTTP oriented, networking protocols in your application.

### Vert.x

[http://vertx.io](http://vertx.io)

Ratpack is similar to Vert.x in some respects: 

* Built on Netty
* Not based on J2EE or Servlets
* Non blocking

A key difference between Vert.x and Ratpack is that Vert.x is a container, where Ratpack is a set of libraries (Vert.x can be embedded, but is typically not).
As such, it attempts to address a broader set of concerns than Ratpack.

Vert.x applications are a composed set of “verticles” communicating via an unstructured JSON (in terms of schema) distributed bus.
Vert.x provides support for many different programming languages, even making it easy to compose an application out of verticles of different languages.
  
Vert.x makes certain irreversible decisions about how applications should be written and architected.
It provides horizontal scaling, a message bus, development time support and more.
In contrast Ratpack is less prescriptive, giving you more freedom (and responsibility) to build your app from the ground up with the technologies you choose.

Vert.x has a plugin system and a healthy ecosystem of plugins, generally collaborating around the message bus.
The [“Architecture”](architecture.html) chapter of this manual discusses why Ratpack does not have a plugin mechanism, and why it doesn't need one.

Vert.x supports many protocols like Netty.
Its API is closer to Netty's, and completely encapsulates it.
As such, it is (not significantly) more performant than Ratpack and more suitable for lower level networking.

Ratpack is more focussed on HTTP applications than Vert.x.
Ratpack provides more support for structuring, composing, evolving and testing request handling logic than Vert.x.
A more apt comparison than Ratpack and Vert.x would be a HTTP oriented framework built on top of Vert.x, such as [Yoke](http://pmlopes.github.io/yoke).
  
Another key difference is that the Vert.x API is callback based.
Ratpack use the concept of a “promise” instead (see the [“Async”](async.html) chapter for more info).
Moreover, Ratpack's [handler pipeline](handlers.html) is designed to support composition of asynchronous functions that process requests (called handlers) without callbacks.
Both Vert.x and Ratpack integrate with [RxJava](https://github.com/Netflix/RxJava) for composition of generic asynchronous functions through add-ons.
See [RxVertx](https://github.com/vert-x/mod-rxvertx), and the [“RxJava”](rxjava.html) chapter of this manual.

Architecturally Vert.x integrates application components via its own implementation of the Actor model (i.e. via message passing on its own event bus).
There is no equivalent Ratpack feature.
You are free to choose your own approach for higher level composition.

### RxNetty

[https://github.com/Netflix/RxNetty](https://github.com/Netflix/RxNetty)

RxNetty is the integration of [RxJava](https://github.com/Netflix/RxJava) and Netty.
It is slightly more than a “reactive” layer over Netty.
It is a young project.
 
A lot of the comparison between Ratpack and Netty given previously is applicable to comparing Ratpack and RxNetty.
Most users of Ratpack will never need to access Netty's API.
Ratpack's API is a different “shape” to Netty's as it is more tightly focussed, therefore comparing the Ratpack and RxNetty APIs is not that useful.

Ratpack provides an [RxJava integration library](rxjava.html) that can be used to bridge Ratpack promises and RxJava observables.
The use of RxJava is recommended for all non trivial Ratpack applications.

### Grails

[http://grails.org](http://grails.org)

Grails is a full stack, fully featured, MVC based web development framework (in the tradition of Ruby on Rails).
It provides many deeply integrated features such as multi-store persistence (GORM), a view technology (GSP),
a proprietary build system, testing support and has a vast ecosystem of plugins. 
 
Grails does _much_ more than Ratpack does.
It is a framework and has very strong opinions about how to write and structure applications.
It is based on the Servlet architecture and leverages the Spring Framework.

Grails is Groovy based, both in terms of internal implementation and language of choice for adopters.
Ratpack is implemented in 100% Java, but provides a [small adapter layer for Groovy](groovy.html).
Ratpack is not a build system and makes no restrictions on language of choice for adopters other than providing functional Java APIs 
(which can easily be used from other JVM languages that bridge to Java).

Ratpack is more performant than Grails.
However, Grails provides much more functionality.

### Spark

[http://www.sparkjava.com](http://www.sparkjava.com)

Spark is a micro web framework. 
It is in the tradition of [Ruby's Sinatra](http://www.sinatrarb.com).

> At this point the question has to be asked about the relationship between Sinatra and Ratpack.
>
> Ratpack was originally a Groovy based clone of Sinatra.
> It has since transformed from this and could no longer reasonably be considered to be in the tradition of Sinatra clones.
 
Spark is ultimately very simple and less ambitious than Ratpack.
Spark is a blocking framework and does not enjoy the performance of Netty.
It is Servlet based, but encapsulates the Servlet API.
 
Ratpack aims to scale to very large applications, both in terms of throughput and complexity.
It achieves performance through its use of Netty (and non-blocking) and supports large applications through its coherent and non invasive API. 


### Dropwizard

[https://dropwizard.github.io/dropwizard](https://dropwizard.github.io/dropwizard)

TBD.

### Play Framework
[https://playframework.com/](https://playframework.com/)

Ratpack and Play share a few commonalities:

* Netty Based (Play uses Netty 3.9, Ratpack uses Netty 4.X)
* Async from the ground up
* [Reactive](http://www.reactivemanifesto.org/)

The biggest differences are found in request handling, SDK, Netty version, async execution, and deployment.

#### Request Handling
Play follows the traditional MVC approach of handling requests, i.e. an incoming request gets mapped to a controller
which then processes your request and sends back a response.

Ratpack has no concept of controllers. All of Ratpack's request handling is processed via a [chain](api/ratpack/handling/Chain.html)
of [handlers](api/ratpack/handling/Handler.html)

#### SDK
Play requires you to download an SDK to develop Play apps.
Two active Play distributions are maintained

* Play 1.X Java core - uses python script to execute sdk commands
* Play 2.X Scala core with optional Java API - requires sbt

You do not need to download any SDK to develop Ratpack apps. In fact a Ratpack app can be as simple as a 
[standalone groovy script](https://github.com/ratpack/example-ratpack-standalone-groovy-script/blob/master/ratpack.groovy).

Ratpack is purely Java 8 based with first class Groovy support.


#### Netty 3.9 vs 4.X
Trustin Lee, a core Netty developer at Twitter, wrote an article about performance increase [migrating from Netty 3.X to 4.X](https://blog.twitter.com/2013/netty-4-at-twitter-reduced-gc-overhead).
To summarize Netty 4 has significantly less garbage production and less frequent GC pauses.
[More indepth summary of Netty 4 vs Netty 3 can be found here](http://netty.io/wiki/new-and-noteworthy-in-4.0.html).


#### Async Execution
When working with an asynchronous framework, you need to be able to reason about the application's execution.
You also need to be aware of the execution model so that you are not misusing the framework's resources.
Most libraries, e.g. JDBC, are blocking and need to be integrated into a non-blocking world. How this is accomplished
is a detail of the framework.

You'll need some way to hook into the framework's non-cpu bound thread pool to execute long running tasks.

This is [non-trivial in Play](https://playframework.com/documentation/2.3.x/ThreadPools) and requires that you
understand what each provided thread pool does so that you are using the Play's resources optimally.

Play also warns:
> Note that you may be tempted to therefore wrap your blocking code in Futures. This does not make it non blocking, it just means the blocking will happen in a different thread. You still need to make sure that the thread pool that you are using there has enough threads to handle the blocking.

In order to integrate traditional blocking APIs into your app, you'll need to understand the nature of
each provided thread pool.

To drive the point home, [Play's documentation states](https://playframework.com/documentation/2.3.x/JavaAsync)
> You can’t magically turn synchronous IO into asynchronous by wrapping it in a Promise.

In Ratpack you can via the [ExecController](api/ratpack/exec/ExecControl.html#promise-ratpack.func.Action-).

In an async world, you do not know when various asynchronous tasks will complete. This leads to debugging induced
headaches.

Compare these two code samples

Play async code example

```
package controllers;

import play.libs.F;
import play.mvc.*;

public class Application extends Controller {
  public static F.Promise<Result> index() throws InterruptedException {
    System.out.println("1");
  
    F.Promise<String> promise1 = F.Promise.promise(() -> {
      System.out.println("2");
      return "foo";
    });
  
    Thread.sleep(10000);
  
    System.out.println("4");
    return promise1.map(s -> {
      System.out.println("3");
      return ok(s);
    });
  }
}
```

Ratpack async code example from [Luke Daley's Blog](http://ldaley.com/post/97376696242/ratpack-execution-model-part-1)

```language-groovy
handlers {
  get {
    print "1"
    blocking {
      print "2"
      getValueFromDb() // returns string
    }.then {
      print "3"
      render it // it is the return of getValueFromDb() 
    }
    sleep 10000
    print "4"
  }
}
```

From Ratpack's execution model the execution order of the Ratpack code is guaranteed to be 1,4,2,3.
Play does not guarantee any kind of execution order as it depends on how long it takes for each promise
to resolve.

#### Deployment
Deployment: Play suggests to use the Play SDK to run the application in production, Ratpack does not force you to use anything.
Standard way of distributing and deploying Ratpack apps is a fatjar. It should be noted that Play offers a way to [create a standalone distribution](https://www.playframework.com/documentation/2.3.x/ProductionDist).


## How to read the documentation

The canonical reference documentation for Ratpack is the [Javadoc](api/).
This manual acts as a starting point for different aspects of Ratpack and as a mechanism to pull together larger ideas and patterns.
The manual frequently introduces topics at a high level and links through to the Javadoc for detailed API information.
Much work goes in to making the Javadoc (hopefully) useful.

## Project Status

Ratpack is a new project, but stands on the shoulders of well established technologies and is built by developers with substantial experience in web application frameworks and tooling. 
It is currently pre 1.0 but under very active development.

No API compatibility guarantees are provided before the 1.0 release, from which point on the API will be strictly semantically versioned.

While it is new, it has received many man hours of development time.
