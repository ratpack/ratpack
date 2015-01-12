# Basic HTTP

This chapter introduces how to deal with basic HTTP concerns such as parsing requests, rendering responses, content negotiation file uploads etc.

## Request & Response

The context object that a handler operates on provides the ([`getRequest()`](api/ratpack/handling/Context.html#getRequest--)
& [`getResponse()`](api/ratpack/handling/Context.html#getResponse--) methods for accessing the [`Request`](api/ratpack/http/Request.html) and [`Response`](api/ratpack/http/Response.html) respectively.
These objects provide more or less what you would expect. 

For example, they both provide a `getHeaders()` method that returns an model of the HTTP headers sent with the request and a model of the HTTP headers that are to be sent with the response.
The [`Request`](api/ratpack/http/Request.html) exposes other metadata attributes such as the [HTTP method](api/ratpack/http/Request.html#getMethod--),
the [URI](api/ratpack/http/Request.html#getUri--) and a key/value model of the [query string parameters](api/ratpack/http/Request.html#getQueryParams--) among other things.

## Reading the request

Manually handling request bodies can be tedious and error prone. Ratpack provides a [`Parser`](api/ratpack/parse/Parser.html) that converts an HTTP request body 
into a Java object. The `Parser` is the mechanism that powers the context's [`parse()`](api/ratpack/handling/Context.html#parse-java.lang.Class-) method.
Ratpack provides two parsable types:

* [`Form`](api/ratpack/form/Form.html) for parsing form submission data
* [`JsonNode`](http://fasterxml.github.io/jackson-databind/javadoc/2.4/com/fasterxml/jackson/databind/JsonNode.html) for parsing JSON

To use the JSON parser you'll need to use the [`JacksonModule`](api/ratpack/jackson/JacksonModule.html) provided by the `ratpack-jackson` module.
In addition to the `JsonNode`, the `ratpack-jackson` module also provides a way to convert POJOs into parsable types via [`Jackson.fromJson()`](api/ratpack/jackson/Jackson.html#fromJson-java.lang.Class-).
This allows you to parse incoming JSON to POJOs in your Handlers. See [`Jackson`](api/ratpack/jackson/Jackson.html#parsing) for examples of JSON parsing.

### Forms

Ratpack provides a parser for [`Form`](api/ratpack/form/Form.html) objects in the core.
This can be used for reading POST'd (or PUT'd etc. for that matter) forms, both URL encoded and multi part (including file uploads).

Here's an example of using this from Java…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.form.Form;
import ratpack.form.UploadedFile;

public class MyHandler implements Handler {
  public void handle(Context context) {
    Form form = context.parse(Form.class);

    // Get the first attribute sent with name “foo”
    String foo = form.get("foo");

    // Get all attributes sent with name “bar”
    List<String> bar = form.getAll("bar");

    // Get the file uploaded with name “myFile”
    UploadedFile myFile = form.file("myFile");

    // Send back a response …
  }
}
```

See [`Form`](api/ratpack/form/Form.html) and [`UploadedFile`](api/ratpack/form/UploadedFile.html) for more information and examples.

## Sending a response

TODO introduce status methods

TODO introduce send methods

TODO introduce renderers

TODO introduce sendFile methods (pointing to use of `render(file(«path»)))` instead.

TODO introduce beforeSend method and the ResponseMetaData interface.

## Cookies

TODO introduce getCookies() on request and response 

TODO introduce Request#oneCookie()

TODO introduce Response#expireCookie()

## Sessions

TODO introduce ratpack-sessions library
