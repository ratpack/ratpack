# Streams

Ratpack supports streaming data in a variety of ways.
This chapter outlines the fundamentals of working with data streams in Ratpack and different ways to stream data.

## The Reactive Streams API

Generally, streaming in Ratpack is based around the emerging [Reactive Streams](http://www.reactive-streams.org) API standard.

From the Reactive Streams site:

> Reactive Streams is an initiative to provide a standard for asynchronous stream processing with non-blocking back pressure on the JVM.

Ratpack uses the Reactive Streams API, opposed to a proprietary API, to allow users to choose their reactive toolkit of choice.
Reactive toolkits such as [RxJava](rxjava.html) and [Reactor](https://github.com/reactor/reactor) will support bridging to the Reactive Streams API in the near future.
However, it is not required to use a specialist reactive library if your needs are modest.
Ratpack provides some useful utilities for dealing with streams via its [`Streams`](api/ratpack/stream/Streams.html) class.

### Back pressure

A key tenet of the Reactive Streams API is support for flow control via back pressure.
This allows stream subscribers, which in the case of a HTTP server app is usually the HTTP client, to communicate to the publisher how much data they can handle.
In extreme cases, without back pressure a slowly consuming client can exhaust resources on the server as the data producer produces data faster than it is being consumed, potentially filling up in memory buffers.
Back pressure allows the data producer to match its rate of production with what the client can handle.
   
For more info on the importance of back pressure, please see the documentation from the [Reactive Streams](http://www.reactive-streams.org) project.
 
Streaming a response always occurs via the [`Response.sendStream()`](api/ratpack/http/Response.html#sendStream-org.reactivestreams.Publisher-) method.
See the documentation for this method for more precise semantics of what back pressure means when streaming data.
 
## Chunked transfer encoding

Ratpack supports [chunked transfer encoding](http://en.wikipedia.org/wiki/Chunked_transfer_encoding) for arbitrary data streams by way of the [`ResponseChunks`](api/ratpack/http/ResponseChunks.html) renderable type.

## Server-sent events

Ratpack supports [server-sent events](https://developer.mozilla.org/en-US/docs/Server-sent_events/Using_server-sent_events) for streaming data to, primarily Javascript based, clients by way of the [`ServerSentEvents`](api/ratpack/sse/ServerSentEvents.html) renderable type.

## Websockets

Ratpack supports streaming data over [websockets](http://en.wikipedia.org/wiki/WebSocket) by way of the [`WebSockets.websocketBroadcast()`](api/ratpack/websocket/WebSockets.html#websocketBroadcast-ratpack.handling.Context-org.reactivestreams.Publisher-) method.
 
Ratpack also supports bidirectional websocket communication via the other websocket-opening methods of the [`WebSockets`](api/ratpack/websocket/WebSockets.html) class.   
