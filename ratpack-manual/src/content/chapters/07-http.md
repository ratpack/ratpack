# Basic HTTP

This chapter introduces how to deal with basic HTTP concerns such as parsing requests, rendering responses, content negotiation file uploads etc.

## Request & Response

The context object that a handler operates on provides the ([`getRequest()`](api/ratpack/handling/Context.html#getRequest\(\))
& [`getResponse()`](api/ratpack/handling/Context.html#getResponse\(\)) methods for accessing the [`Request`](api/ratpack/http/Request.html) and [`Response`](api/ratpack/http/Response.html) respectively.
These objects provide more or less what you would expect. 

For example, they both provide a `getHeaders()` method that returns an model of the HTTP headers sent with the request and a model of the HTTP headers that are to be sent with the response.
The [`Request`](api/ratpack/http/Request.html) exposes other metadata attributes such as the [HTTP method](api/ratpack/http/Request.html#getMethod\(\)),
the [URI](api/ratpack/http/Request.html#getUri\(\)) and a key/value model of the [query string parameters](api/ratpack/http/Request.html#getQueryParams\(\)) among other things.

## Reading the request

TODO introduce parsers generally

### Forms

Ratpack provides a [`FormParser`](api/ratpack/form/FormParser.html) implementation in the core.
This can be used for reading POST'd (or PUT'd etc. for that matter) forms, both URL encoded and multi part (including file uploads).

Here's an example of using this from Java…

```language-groovy tested
import ratpack.handling.Handler;
import ratpack.handling.Context;
import ratpack.form.Form;
import ratpack.form.UploadedFile;

import static ratpack.parse.NoOptParse.to;

public class MyHandler implements Handler {
  public void handle(Context context) {
    Form form = context.parse(to(Form.class));

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

See [`Form`](api/ratpack/form/Form.html), [`UploadedFile`](api/ratpack/form/UploadedFile.html) and [`Forms`](api/ratpack/form/Forms.html) for more information and examples.

## Sending a response

TODO introduce status methods

TODO introduce send methods

TODO introduce renderers

TODO introduce sendFile methods (pointing to use of `render(file(«path»)))` instead.

## Cookies

TODO introduce getCookies() on request and response 

TODO introduce Request#oneCookie()

TODO introduce Response#expireCookie()

## Sessions

TODO introduce ratpack-sessions library