# Basic HTTP

This chapter introduces how to deal with basic HTTP concerns such as parsing requests, rendering responses, content negotiation, file uploads etc.

## Request & Response

The context object that a handler operates on provides the [`getRequest()`](api/ratpack/handling/Context.html#getRequest--)
& [`getResponse()`](api/ratpack/handling/Context.html#getResponse--) methods for accessing the [`Request`](api/ratpack/http/Request.html) and [`Response`](api/ratpack/http/Response.html) respectively.
These objects provide more or less what you would expect. 

For example, they both provide a `getHeaders()` method that returns a model of the HTTP headers sent with the request and a model of the HTTP headers that are to be sent with the response.
The [`Request`](api/ratpack/http/Request.html) exposes other metadata attributes such as the [HTTP method](api/ratpack/http/Request.html#getMethod--),
the [URI](api/ratpack/http/Request.html#getUri--) and a key/value model of the [query string parameters](api/ratpack/http/Request.html#getQueryParams--) among other things.

## Redirecting

The [`redirect(int, Object)`](api/ratpack/handling/Context.html#redirect-int-java.lang.Object-) context method supports issuing redirects.
This method obtains the [`Redirector`](api/ratpack/handling/Redirector.html) from the context registry and forwards the arguments.

Ratpack provides a [default implementation](api/ratpack/handling/Redirector.html#standard--) that supports: 

1. Literal URL values
2. Protocol relative URL values
3. Absolute paths within the current application
4. Relative paths within the current application

Most applications do not need to provide a custom `Redirector` implementation, as the default behaviour is sufficient.
One reason to provide a custom redirector implementation would be to interpret domain objects as locations to redirect to.

## Reading the request

Several mechanisms are available for obtaining the body of a request.
For simple use cases, [`Context.parse(Class<T>)`](api/ratpack/handling/Context.html#parse-ratpack.parse.Parse-) will buffer the entire class into memory and yield an object of the specified type.
When you just need a text or byte view of the entire request, you may use the lower-level [`Request.getBody()`](api/ratpack/http/Request.html#getBody--) method.
For advanced uses or for handling extra large requests, [`Request.getBodyStream()`] provides access to the individual byte chunks as they are recieved.

### Parsers

The parser mechanism to turn the request body into an object representation. 
It works by selecting a [`Parser`](api/ratpack/parse/Parser.html) implementation from context registry.
See [`Context.parse(Class<T>)`](api/ratpack/handling/Context.html#parse-ratpack.parse.Parse-) for details and additional variants. 

#### JSON

Support for dealing with JSON request bodies is provided out of the box, based on Jackson.
See [`Jackson parsing`](api/ratpack/jackson/Jackson.html#parsing) for examples.

#### Forms

Ratpack provides a parser for [`Form`](api/ratpack/form/Form.html) objects in the core.
This can be used for reading POST'd (or PUT'd etc. for that matter) forms, both URL encoded and multi part (including file uploads).

```language-java tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.form.Form;
import ratpack.form.UploadedFile;

public class MyHandler implements Handler {
  public void handle(Context context) {
    Promise<Form> form = context.parse(Form.class);

    form.then(f -> {
      // Get the first attribute sent with name “foo”
      String foo = form.get("foo");

      // Get all attributes sent with name “bar”
      List<String> bar = form.getAll("bar");

      // Get the file uploaded with name “myFile”
      UploadedFile myFile = form.file("myFile");

      // Send back a response …
    });
  }
}
```

See [`Form`](api/ratpack/form/Form.html) and [`UploadedFile`](api/ratpack/form/UploadedFile.html) for more information and examples.

### Bytes and Text

[`Request.getBody()`](api/ratpack/http/Request.html#getBody--) reads the entire request into memory, providing access to the data as either bytes or a string.

This method will default to rejecting requests which are larger than the server's configured [max content length](api/ratpack/server/ServerConfig.html#getMaxContentLength--).
Additional flavors are available for configuring the [rejection action](api/ratpack/http/Request.html#getBody-ratpack.func.Block-) and the [maximum size](api/ratpack/http/Request.html#getBody-long-).

```language-java tested
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> {
         ctx.getRequest().getBody().then(data -> ctx.render("hello: "+data.getText()));
      })
      .test(httpClient -> {
        ReceivedResponse response = httpClient.request(req->{
          req.method("POST");
          req.getBody().text("world");
        });
        assertEquals("hello: world", response.getBody().getText());
      });
  }
}
```

### Byte Chunk Stream

[`Request.getBodyStream()`](api/ratpack/http/Request.html#getBodyStream--) returns a stream of the individual chunks as they are received.

This method defaults to rejecting requests which are larger than the server's configured [max content length](api/ratpack/server/ServerConfig.html#getMaxContentLength--).
Additional flavors are available for configuring the [maximum size](api/ratpack/http/Request.html#getBodyStream-long-).

See the [java docs](api/ratpack/http/Request.html#getBodyStream-long-) for an example of how to stream the request body to a file.

## Sending a response

Sending a HTTP response in Ratpack is easy, efficient, and flexible.
Like most things in Ratpack, transmitting a response to a client is done in a non-blocking manner.
Ratpack provides a few mechanisms for sending a response.
The methods exposed to manipulating the response can be found in the [`Response`](api/ratpack/http/Response.html) and [`Context`](api/ratpack/handling/Context.html) objects. 


### Setting the response status

Setting the status of a response is as easy as calling [`Response#status(int)`](api/ratpack/http/Response.html#status-int-) or [`Response#status(ratpack.http.Status)`](api/ratpack/http/Response.html#status-ratpack.http.Status-).

```language-java
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandlers(chain -> chain
      .all(ctx -> ctx.getResponse().status(202).send("foo"))
    )
    .test(httpClient ->
      assertEquals(202, httpClient.get().getStatusCode())
    );
  }
}
```

### Sending the response

There are a few ways to send a response body to the client.

The shortest way to send a response is to simply call [`Response#send()`](api/ratpack/http/Response.html#send--).
This will send a response with no response body.

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> ctx.getResponse().send())
      .test(httpClient -> {
        ReceivedResponse response = httpClient.get();
        assertEquals("", response.getBody().getText());
      });
  }
}
```


If you want to send a plain text response you can use [`Response#send(String)`](api/ratpack/http/Response.html#send-java.lang.String-).

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> ctx.getResponse().send("Ratpack is rad"))
      .test(httpClient -> {
        ReceivedResponse response = httpClient.get();
        assertTrue(response.getHeaders().get("Content-type").startsWith("text/plain;"));
        assertEquals("Ratpack is rad", response.getBody().getText());
      });
  }
}
```

There are additional `send()` methods that allow you send different the response body payloads, i.e. `String`, `byte[]`, `ByteBuf`, as well as set the `Content-type` header.
See [`Response`](api/ratpack/http/Response.html) for more on sending a response.


### An alternative approach with Renderers

Sending empty or simple text responses may be fine but you may find yourself wanting to send a more complex response to the client.
The [`Renderer`](api/ratpack/render/Renderer.html) is a mechanism that is able to render a given type to the client.
More specifically, it's the underlying mechanism that powers the [`render(Object)`](api/ratpack/handling/Context.html#render-java.lang.Object-) method, which can be found on the context object.

In the following example, we utilize the context's `render(Object)` method to render an object of type `String`.

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> ctx.render("Sent using render(Object)!"))
      .test(httpClient -> {
        ReceivedResponse response = httpClient.get();
        assertEquals("Sent using render(Object)!", response.getBody().getText());
      });
  }
}
```

Because the `String` is of type `CharSequence`, Ratpack finds and uses the `CharSequenceRenderer` to render the `String`.
Where did this `CharSequenceRenderer` come from?
Ratpack provides a number of `Renderer`s out of the box, including but not limited to: `CharSequenceRenderer`, `RenderableRenderer`, `PromiseRenderer`, `DefaultFileRenderer`.

If you attempt to render a type that is not registered, it will result in a server error.

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {

  static class Foo {
    public String value;
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> {
        Foo foo = new Foo();
        foo.value = "bar";
        ctx.render(foo);
      })
      .test(httpClient -> {
        ReceivedResponse response = httpClient.get();
        assertEquals(500, response.getStatusCode());
      });
  }
}
```

If you'd like to implement your own `Renderer`, Ratpack provides a [`RendererSupport`](api/ratpack/render/RendererSupport.html) that makes it easy to implement your own.
You must also remember to register your `Renderer` so that Ratpack can use it.

```language-java
import ratpack.handling.Context;
import ratpack.registry.Registry;
import ratpack.http.client.ReceivedResponse;
import ratpack.render.RendererSupport;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {

  static class Foo {
    public String value;
  }

  static class FooRenderer extends RendererSupport<Foo> {
    @Override
    public void render(Context ctx, Foo foo) throws Exception {
      ctx.getResponse().send("Custom type: Foo, value=" + foo.value);
    }
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandlers(chain -> chain
        .register(Registry.single(new FooRenderer()))
        .all(ctx -> {
          Foo foo = new Foo();
          foo.value = "bar";
          ctx.render(foo);
        })
      )
      .test(httpClient -> {
        ReceivedResponse response = httpClient.get();
        assertEquals(200, response.getStatusCode());
        assertEquals("Custom type: Foo, value=bar", response.getBody().getText());
      });
  }
}
```

### Sending JSON

Support for rendering arbitrary objects as JSON is based on Jackson.
See [`Jackson rendering`](api/ratpack/jackson/Jackson.html#rendering) for examples.

### Sending files

Sending static resources such as files can be done with [`sendFile(Path)`](api/ratpack/http/Response.html#sendFile-java.nio.file.Path-) 

TODO introduce sendFile methods (pointing to use of `render(file(«path»)))` instead.

TODO introduce assets method

### Before send

The [`Response`](api/ratpack/http/Response.html) object contains a method, `beforeSend(Action<? super Response> responseFinalizer)`, that is invoked immediately before a Response is sent to the client.

You may not call any of the `.send()` methods on the `Response` within the `responseFinalizer` callback.

This method is particularly useful for modifying

* headers
* cookies
* the status
* the content-type

at the last second.

A practical use-case for this would be modifying the status code or headers when using [`StreamedResponse.forwardTo()`](api/ratpack/http/client/StreamedResponse.html).

For example:

```language-java
import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;
import ratpack.http.Headers;
import ratpack.http.Request;
import ratpack.http.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> {
        ctx.getResponse()
          .contentType("application/json")
          .status(Status.OK)
          .beforeSend(response -> {
             response.getHeaders().remove(HttpHeaderNames.CONTENT_LENGTH);
             response.cookie("DNT", "1");
             response.status(Status.of(451, "Unavailable for Legal Reasons"));
             response.contentType("text/plain");
          }).send();
      })
      .test(httpClient -> {
        ReceivedResponse receivedResponse = httpClient.get();

        Headers headers = receivedResponse.getHeaders();
        assertEquals(451, receivedResponse.getStatusCode());
        assertEquals("text/plain", headers.get(HttpHeaderNames.CONTENT_TYPE));
        assertTrue(headers.get(HttpHeaderNames.SET_COOKIE).contains("DNT"));
      });
  }
}
```

## Headers

HTTP Header information is available from an incoming request as it is for an outgoing response.

### Request headers

The [`Headers`](api/ratpack/http/Headers.html) interface allows you to retrieve header information associated with the incoming request.

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;
import ratpack.http.Headers;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> {
        Headers headers = ctx.getRequest().getHeaders();
        String clientHeader = headers.get("Client-Header");
        ctx.getResponse().send(clientHeader);
      })
      .test(httpClient -> {
        ReceivedResponse receivedResponse = httpClient
          .requestSpec(requestSpec ->
              requestSpec.getHeaders().set("Client-Header", "From Client")
          ).get();

        assertEquals("From Client", receivedResponse.getBody().getText());
      });
  }
}
```

### Response headers

The [`MutableHeaders`](api/ratpack/http/MutableHeaders.html) provides functionality that enables you to manipulate response headers via the response object [`Response#getHeaders()`](api/ratpack/http/Response.html#getHeaders--).

```language-java
import ratpack.http.MutableHeaders;
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp
      .fromHandler(ctx -> {
        MutableHeaders headers = ctx.getResponse().getHeaders();
        headers.add("Custom-Header", "custom-header-value");
        ctx.getResponse().send("ok");
      })
      .test(httpClient -> {
        ReceivedResponse receivedResponse = httpClient.get();
        assertEquals("custom-header-value", receivedResponse.getHeaders().get("Custom-Header"));
      });
  }
}
```

Additionally you can [`set(CharSequence, Object)`](api/ratpack/http/MutableHeaders.html#set-java.lang.CharSequence-java.lang.Object-), [`remove(CharSequence)`](api/ratpack/http/MutableHeaders.html#remove-java.lang.CharSequence-), [`clear()`](api/ratpack/http/MutableHeaders.html#clear--) and more.

See [`MutableHeaders`](api/ratpack/http/MutableHeaders.html) for more methods.


## Cookies

As with HTTP headers, cookies are available for inspection from an inbound request as they are for manipulation for an outbound response.

### Cookies from an inbound request

To retrieve the value of a cookie, you can use [`Request#oneCookie(String)`](api/ratpack/http/Request.html#oneCookie-java.lang.String-).

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandler(ctx -> {
      String username = ctx.getRequest().oneCookie("username");
      ctx.getResponse().send("Welcome to Ratpack, " + username + "!");
    }).test(httpClient -> {
      ReceivedResponse response = httpClient
        .requestSpec(requestSpec -> requestSpec
          .getHeaders()
          .set("Cookie", "username=hbogart1"))
        .get();

      assertEquals("Welcome to Ratpack, hbogart1!", response.getBody().getText());
    });
  }
}
```

You can also retrieve a set of cookies via [`Request#getCookies()`](api/ratpack/http/Request.html#getCookies--).

```language-java
import io.netty.handler.codec.http.cookie.Cookie;
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandler(ctx -> {
      Set<Cookie> cookies = ctx.getRequest().getCookies();
      assertEquals(1, cookies.size());
      Cookie cookie = cookies.iterator().next();
      assertEquals("username", cookie.name());
      assertEquals("hbogart1", cookie.value());
      ctx.getResponse().send("Welcome to Ratpack, " + cookie.value() + "!");
    }).test(httpClient -> {
      ReceivedResponse response = httpClient
        .requestSpec(requestSpec -> requestSpec
          .getHeaders()
          .set("Cookie", "username=hbogart1"))
        .get();

      assertEquals("Welcome to Ratpack, hbogart1!", response.getBody().getText());
    });
  }
}
```

### Setting cookies for an outbound response

You can set cookies to be sent with the response [`Response#cookie(String, String)`](api/ratpack/http/Response.html#cookie-java.lang.String-java.lang.String-).
To retrieve the set of cookies to be set with the response you may use [`Response#getCookies()`](api/ratpack/http/Response.html#getCookies--).

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandler(ctx -> {
      assertTrue(ctx.getResponse().getCookies().isEmpty());
      ctx.getResponse().cookie("whiskey", "make-it-rye");
      assertEquals(1, ctx.getResponse().getCookies().size());
      ctx.getResponse().send("ok");
    }).test(httpClient -> {
      ReceivedResponse response = httpClient.get();
      assertEquals("whiskey=make-it-rye", response.getHeaders().get("Set-Cookie"));
    });
  }
}
```

If you want to expire a cookie, you can do so with [`Response#expireCookie()`](api/ratpack/http/Response.html#expireCookie-java.lang.String-). 

```language-java
import ratpack.http.client.ReceivedResponse;
import ratpack.test.embed.EmbeddedApp;

import static org.junit.Assert.assertTrue;

public class Example {
  public static void main(String... args) throws Exception {
    EmbeddedApp.fromHandler(ctx -> {
      ctx.getResponse().expireCookie("username");
      ctx.getResponse().send("ok");
    }).test(httpClient -> {
      ReceivedResponse response = httpClient
        .requestSpec(requestSpec -> requestSpec
            .getHeaders().set("Cookie", "username=lbacall1")
        )
        .get();

      String setCookie = response.getHeaders().get("Set-Cookie");
      assertTrue(setCookie.startsWith("username=; Max-Age=0"));
    });
  }
}
```

## Content Negotiation

Support for rendering different representations of a resource (JSON/XML/HTML, GIF/PNG, etc.) is provided via [`byContent(Action)`](api/ratpack/handling/Context.html#byContent-ratpack.func.Action-).

## Sessions

Whenever you need to maintain consistency between multiple calls by the same client (in other words: stateful operations) you may want to use sessions.
Ratpack offers a module to handle session handling for you.
You can add the session module to your project and start using ratpack managed sessions.

### Preparation
First off you need to add the required dependency to your project.
Using gradle you can add the dependency by adding `compile 'io.ratpack:ratpack-session:@ratpack-version@'` to dependencies.
When you just started out your gradle file will look like this:

```language-groovy gradle
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "io.ratpack:ratpack-gradle:@ratpack-version@"
  }
}

apply plugin: "io.ratpack.ratpack-groovy"

repositories {
  jcenter()
}

dependencies {
  runtime 'org.slf4j:slf4j-simple:@slf4j-version@'
  compile group: 'io.ratpack', name: 'ratpack-session', version: '@ratpack-version@'

  testCompile "org.spockframework:spock-core:@spock-version@"
}
```

Don't forget to load the module in ratpack. 

```language-groovy tested
import static ratpack.groovy.Groovy.ratpack
import ratpack.session.SessionModule

ratpack {
	bindings {
		module(SessionModule)
	}
	/* ... */
}
```

### Use session

You are now set for sessions.
The following is a simple example of an application that uses sessions.

```language-groovy tested
import ratpack.session.Session

import static ratpack.groovy.Groovy.ratpack
import ratpack.session.SessionModule

ratpack {
	bindings {
		module(SessionModule)
	}

	handlers {
		get('start') { Session session ->
			session.terminate().flatMap {
				session.set('keyForDataStoredInSession', 'Hello Session!').promise()
			}.then {
				response.send('I started a session for you.')
			}
		}
		get { Session session ->
			session.get('keyForDataStoredInSession').then {
				if (it.present) {
					response.send("Message: ${it.get()}")
				} else {
					response.send('I have nothing to say to you')
				}
			}
		}
		get('stop') { Session session ->
			session.terminate().then {
				response.send('The session is dead, dead, dead.')
			}
		}
	}
}
```

All session operations return either a [Promise](api/ratpack/exec/Promise.html) or an [Operation](api/ratpack/exec/Operation.html).
You can use those in your transformation flow as shown.

Make sure you terminate an old session before starting a new one (see `get('start')`-handler).
This way you make sure that you actually get a new session and don't just add data to an existing session.

### Limitations 

The ratpack session module uses in-memory sessions by default.
It can hold up to 1000 sessions at a time and will drop the oldest session if a new session is opened.
If you expect more than 1000 sessions you should consider to use a different session store than the default module.
You could for example use the `ratpack-session-redis` module if you have a redis server handy.
If your payload is small another option is to store some session data in the cookie itself by using the [client side session module](api/ratpack/session/clientside/ClientSideSessionModule.html).
Your payload should be small because all Cookies for a website (domain) combined cannot exceed 4K.

### The `ratpack-session-redis` module
To use the redis session module add the dependency (`compile 'io.ratpack:ratpack-session-redis:@ratpack-version@'`) to your project.

Then configure redis after loading the session module.

```language-groovy
bindings {
  module(SessionModule)
  RedisSessionModule redisSessionModule = new RedisSessionModule()
  redisSessionModule.configure {
    it.host = 'localhost'
    it.port =  6379
    it.password = 'secret'
  }
  module(redisSessionModule)
}
```

You may want to inject the configuration for redis using Guice or some other mechanism.
Apart from that you have successfully configured ratpack to use redis to store sessions.
Make your your redis server is running and available and give you ratpack application a spin.

### Comments & useful links

You now have a very rough idea of how to implement sessions in ratpack.

Ratpack handles session consistency with cookies, namely a JSESSIONID cookie that contains a UUID.
Whenever you first add data to the session the id is generated and the cookie will be sent to the client using the `Add-Cookie` header in the response.
Therefor you need to make sure that you a response is sent to the client or the session will be lost.
Consecutive client requests contain the cookie in the `Cookie`-Header, so ratpack can determine the client's session.

Here are some useful links to dive deeper into ratpack session handling:
- more samples of how sessions work in the [ratpack-session module tests](https://github.com/ratpack/ratpack/blob/master/ratpack-session/src/test/groovy/ratpack/session/SessionSpec.groovy)
- [ratpack javadoc](api/ratpack/session/SessionModule.html) contains lots of examples and information
- cookie behaviour can be customized (i.e. change name of cookie, use custom ids/expiration date) by providing a custom [SessionCookieConfig](api/ratpack/session/SessionCookieConfig.html) to the session module 

### A final note when you use the default session store (in-memory):

The default session store probably isn't useful in any production environment but it is useful for local testing with sessions.
A call of `session.terminate()` sets the session cookie to an empty value.
Consecutive calls therefor contain a cookie like this `JSESSIONID=`.
At least the in-memory session store accepts the empty value as a valid session.
Therefor if you don't terminate your session before you intend to create a new one by adding values to it, 
you will add session data to the existing session with an empty uuid.
