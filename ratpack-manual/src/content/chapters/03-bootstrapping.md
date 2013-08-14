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
