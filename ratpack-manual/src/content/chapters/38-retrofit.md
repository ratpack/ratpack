# retrofit

The `ratpack-retrofit2` extension provides integration for declarative HTTP clients with the [retrofit2](http://square.github.io/retrofit) library.

The retrofit library allows for representing HTTP APIs via a type-safe interface.
This allows application code to interact with APIs using typed objects and abstracting the HTTP constructs.

Retrofit clients generated using the [`RatpackRetrofit`](api/ratpack/retrofit/RatpackRetrofit.html) class are backed with
Ratpack's [`HttpClient`](api/ratpack/http/client/HttpClient.html) and are capable of interfacing with 
Ratpack's [`Promise`](api/ratpack/exec/Promise.html) construct as a return type.


## Usage

The [`RatpackRetrofit.builder(HttpClient client)`](api/ratpack/retrofit/RatpackRetrofit.html#builder-ratpack.http.client.HttpClient-)
method provides the entry point for creating API clients.
The provided `HttpClient` will be used to issue the underlying HTTP requests for the client.

The builder must be configured with the base URI by calling the [`uri(java.net.URI uri)`](api/ratpack/retrofit/RatpackRetrofit.Builder.html#uri-java.net.URI-)
method before constructing clients.

Once configured, the client is constructed by calling [`build(java.lang.Class api`](api/ratpack/retrofit/RatpackRetrofit.Builder.html#build-java.lang.Class-)
with the api interface. 
This methods returns a generated instance of the interface that will issue the HTTP requests and adapt the responses to the
configured return type.

```language-java
import ratpack.exec.Promise;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.retrofit.RatpackRetrofit;
import ratpack.server.PublicAddress;
import ratpack.test.embed.EmbeddedApp;
import retrofit2.http.GET;

import static org.junit.Assert.*;

public class Example {

  public static interface HelloApi {
    @GET("hi") Promise<String> hello();
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp.of(s -> s
      .handlers(chain -> {
        chain.get(ctx -> {
          PublicAddress address = ctx.get(PublicAddress.class);
          HttpClient httpClient = ctx.get(HttpClient.class);
          
          HelloApi api = RatpackRetrofit.builder(httpClient)
            .uri(address.get())
            .build(HelloApi.class);
            
          ctx.render(api.hello());
        });
        chain.get("hi", ctx -> ctx.render("hello"));
      })
    ).test(httpClient -> {
      ReceivedResponse response = httpClient.get();
      assertEquals("hello", response.getBody().getText());
    });
  }
}
```

## Creating multiple API implementations

Many APIs may be represented using distinct interfaces for different capabilities. 
To create multiple clients, the underlying [`Retrofit`](https://square.github.io/retrofit/2.x/retrofit/retrofit2/Retrofit.html) class can be obtained.

```language-java
import ratpack.exec.Promise;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.retrofit.RatpackRetrofit;
import ratpack.server.PublicAddress;
import ratpack.test.embed.EmbeddedApp;
import retrofit2.http.GET;
import retrofit2.Retrofit;

import static org.junit.Assert.*;

  
public class Example {

  public static interface HelloApi {
    @GET("hi") Promise<String> hello();
  }
  
  public static interface GoodbyeApi {
    @GET("bye") Promise<String> bye();
  }

  public static void main(String... args) throws Exception {
    EmbeddedApp.of(s -> s
      .handlers(chain -> {
        chain.get(ctx -> {
          PublicAddress address = ctx.get(PublicAddress.class);
          HttpClient httpClient = ctx.get(HttpClient.class);
          
          Retrofit retrofit = RatpackRetrofit.builder(httpClient)
            .uri(address.get())
            .retrofit();
            
          HelloApi hiApi = retrofit.create(HelloApi.class);
          GoodbyeApi byeApi = retrofit.create(GoodbyeApi.class);
            
          ctx.render(hiApi.hello().right(byeApi.bye()).map(p -> p.left() + " and " + p.right()));
        });
        chain.get("hi", ctx -> ctx.render("hello"));
        chain.get("bye", ctx -> ctx.render("goodbye"));
      })
    ).test(httpClient -> {
      assertEquals("hello and goodbye", httpClient.getText());
    });
  }
}
```


