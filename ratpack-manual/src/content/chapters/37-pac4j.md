# Pac4j

The `ratpack-pac4j` extension provides authentication support via integration with [pac4j](https://github.com/pac4j/pac4j).

The pac4j library is an authentication engine which abstracts over different authentication protocols such as OAuth, CAS, OpenID (Connect), SAML, Google App Engine and HTTP (form and basic auth).
It is also possible to use custom authentication mechanisms (e.g. database backed). 

The [`RatpackPac4j`](api/ratpack/pac4j/RatpackPac4j.html) class provides the entirety of the integration.
This class provides static methods that provide handler implementations along with other finer grained constructs for use within your handler implementations.
The API reference for this class provides usage examples of each of the methods.

The `ratpack-pac4j` library requires the `ratpack-session` module, and use of the associated [`SessionModule`](api/ratpack/session/SessionModule.html).

## Usage

Each authentication mechanism in `pac4j` is defined as a [Client](https://github.com/pac4j/pac4j/blob/master/pac4j-core/src/main/java/org/pac4j/core/client/Client.java).
For example, Pac4j provides the [FacebookClient](http://www.pac4j.org/apidocs/pac4j/org/pac4j/oauth/client/FacebookClient.html) type that implements the Facebook authentication protocol.
Your application must expose an “authenticator” endpoint, that given a user and an intended “client”, authenticates the user.
The [`RatpackPac4j.authenticator(Client<?, ?>... clients)`](api/ratpack/pac4j/RatpackPac4j.html#authenticator-org.pac4j.core.client.Client...-) provides a handler implementation to act as this endpoint.
When an authenticated user is required within your application, the user will be redirected to the “authenticator”.

There are two ways to require authentication:

1. [`RatpackPac4j.requireAuth()`](api/ratpack/pac4j/RatpackPac4j.html#requireAuth-java.lang.Class-) - a handler implementation that acts as a “filter”
1. [`RatpackPac4j.login()`](api/ratpack/pac4j/RatpackPac4j.html#login-ratpack.handling.Context-java.lang.Class-) - method that initiates login if required (to be used within a handler implementation)

The [`RatpackPac4j.userProfile()`](api/ratpack/pac4j/RatpackPac4j.html#userProfile-ratpack.handling.Context-) method can be used to obtain the user profile if the user is logged in, without requiring authentication.

## Session Usage

As previously mentioned, using `ratpack-pac4j` requires session support via `ratpack-session`.
When authenticated, the user's profile is stored in the session.
Therefore, terminating the session will effectively log the user out.

## Demo application

Please see the [ratpack-pac4j-demo](https://github.com/pac4j/ratpack-pac4j-demo) application for a complete application that demonstrates how to use pac4j with Ratpack. 
