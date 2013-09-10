# Bootstrapping

This chapter introduces how to bootstrap a Ratpack application.

## Launch configuration

It is very simple to start a Ratpack application.

1. Create a [`LaunchConfig`](api/org/ratpackframework/launch/LaunchConfig.html) instance
2. Create a [`RatpackServer`](api/org/ratpackframework/server/RatpackServer.html) based on the launch config
3. Start the server

Depending on the “mode” of your Ratpack application, this process may vary or may be practically invisible. 
For example, the [`RatpackMain`](api/org/ratpackframework/launch/RatpackMain.html) class provides an application entry point that performs this bootstrapping based on conventions.

See the chapter on “bootstrapping” for more detail. - TODO: write this chapter

## Serving content over HTTPS

In order to have your Ratpack application serve content over HTTPS you need to configure an [`SSLContext`](http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLContext.html) and add it to your launch config. When the application runs it will then accept HTTPS traffic rather than HTTP.

The easiest way to do this is if your SSL context is based on a password protected *.keystore* file. If that is the case you can use the [`LaunchConfigBuilder.ssl`](api/org/ratpackframework/launch/LaunchConfigBuilder.html#ssl%28java.io.File,%20java.lang.String%29) method to tell Ratpack to load the keystore. For example:

```language-groovy
launchConfig {
  ssl getClass().getResource("my-keystore.keystore"), myKeystorePassword
}
```

The first argument to the `ssl` method can be a `File`, a `URL` (as in the example) or an `InputStream`. The second argument is the password for the keystore.

You can also configure a password-protected keystore using a *ratpack.properties* file. Set the path or URI of the keystore file with `ratpack.ssl.keystore.file` and the password with `ratpack.ssl.keystore.password`.

If you want to configure the SSL context yourself you can use the [`LaunchConfigBuilder.sslContext`](api/org/ratpackframework/launch/LaunchConfigBuilder.html#sslContext%28javax.net.ssl.SSLContext%29) method to add it to your launch config. For example:

```language-groovy
launchConfig {
  def mySSLContext = SSLContext.getInstance("TLS")
  // configure mySSLContext...
  sslContext mySSLContext
}
```

There is a useful guide to [*Setting up SSL with Netty*](http://maxrohde.com/2013/09/07/setting-up-ssl-with-netty/) at the *Missing Link* blog that explains how to create, sign and install an SSL certificate and keystore file.