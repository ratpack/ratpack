# pac4j

The pac4j library is a security engine which abstracts over different authentication protocols such as OAuth, CAS, OpenID (Connect), SAML, Google App Engine and HTTP (form and basic auth) as well as custom authentication mechanisms (e.g. database backed).
It also supports various authorization mechanisms: roles / permissions checks, CSRF token, security headers, etc.
Integration with Ratpack is provided via the [pac4j/ratpack-pac4j](https://github.com/pac4j/ratpack-pac4j) maintained by the pac4j community.

Gradle Dependency:

```
implementation 'org.pac4j:ratpack-pac4j:3.0.0'
```

### Session Usage

As previously mentioned, using `ratpack-pac4j` requires session support via `ratpack-session`.
When authenticated, the user's profile is stored in the session.
Therefore, terminating the session will effectively log the user out.

## Demo application

Please see the [ratpack-pac4j-demo](https://github.com/pac4j/ratpack-pac4j-demo) application for a complete application that demonstrates how to use pac4j with Ratpack. 
