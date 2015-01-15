# Http client

Ratpack provides its own [`HttpClient`](api/ratpack/http/client/HttpClient.html) which can be used to make remote HTTP calls.
The Ratpack provided `HttpClient` is fully non-blocking and is a part of the core Ratpack library.
Just like the Ratpack server, the `HttpClient` also uses Netty under the hood and in fact shares the same [`EventLoopGroup`](http://netty.io/4.1/api/io/netty/channel/EventLoopGroup.html) as per Netty best practices.


## Basic GET request


```language-java tested
import ratpack.http.client.HttpClient;
import ratpack.test.embed.EmbeddedApp;

public class HttpClientExample {
  public static void main(String... args) throws Exception {
    try(EmbeddedApp remoteApp = EmbeddedApp.fromHandler(ctx -> ctx.render("Hello from remote"))) { 
      EmbeddedApp.fromHandler(ctx -> ctx
          .get(HttpClient.class)
          .get(remoteApp.getAddress())
          .then(response -> ctx.render(response.getBody().getText()))
      ).test(httpClient -> {
        assert httpClient.getText().equals("Hello from remote");
      });
    }
  }
}

```
