/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.http

import ratpack.exec.Blocking
import ratpack.http.client.RequestSpec
import ratpack.registry.Registry
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler
import spock.util.concurrent.BlockingVariable

class RequestBodyReadingSpec extends RatpackGroovyDslSpec {

  def "can not read body"() {
    when:
    serverConfig {
      development true
    }
    handlers {
      post("ignore") {
        render "ok"
      }
      post("read") {
        request.body.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText("ignore") == "ok"
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "bar" }) }
    postText("read") == "bar"
  }

  def "can read body after redirect"() {
    when:
    serverConfig {
      development true
    }
    handlers {
      post("redirect") {
        redirect 303, "read"
      }
      post("read") {
        request.body.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText("redirect") == "foo"
  }

  def "can get request body in development"() {
    when:
    serverConfig {
      development true
    }
    handlers {
      post {
        request.body.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "can get request body as bytes"() {
    when:
    handlers {
      post {
        request.body.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "can get request body async"() {
    when:
    handlers {
      post {
        Blocking.get { "foo " }.flatMap { request.body }.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "can get request body as input stream"() {
    when:
    handlers {
      post {
        request.body.then { body ->
          response.send new String(body.inputStream.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { it.body.stream { it << "foo".getBytes("utf8") } }
    postText() == "foo"
  }

  def "can get large request body as bytes"() {
    given:
    def string = "a" * 1024 * 9

    when:
    handlers {
      post {
        request.body.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { requestSpec ->
      requestSpec.body.stream({ it << string.getBytes("utf8") })
    }
    postText() == string
  }

  def "get bytes on get request"() {
    when:
    handlers {
      all {
        request.body.then { body ->
          response.send body.bytes.length.toString()
        }
      }
    }

    then:
    getText() == "0"
    postText() == "0"
    putText() == "0"
  }

  def "respect max content length"() {
    when:
    serverConfig {
      maxContentLength 16
    }
    handlers {
      post {
        request.body.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "bar".multiply(16) }) }
    def response = post()
    response.statusCode == 413
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "override acceptable max content length per request"() {
    when:
    serverConfig {
      maxContentLength 16
    }
    handlers {
      post {
        request.body.then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
      post("allow") {
        request.getBody(16 * 8).then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "bar".multiply(16) }) }
    def response = post()
    response.statusCode == 413
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo".multiply(16) }) }
    postText("allow") == "foo".multiply(16)
  }

  def "can read body only once"() {
    when:
    handlers {
      all {
        request.body.then { body ->
          next(Registry.single(String, new String(body.bytes, "utf8")))
        }
      }
      post {
        response.send get(String)
      }
      post("again") {
        request.body.then { body ->
          response.send get(String) + new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
    def response = post("again")
    response.statusCode == 500
  }

  def "can eagerly release body"() {
    when:
    handlers {
      post {
        request.body.then { it.buffer.release(); render "ok" }
      }
    }

    then:
    requestSpec {
      it.body.text("foo")
    }
    postText() == "ok"
  }

  def "can't read body twice"() {
    when:
    bindings {
      bind SimpleErrorHandler
    }
    handlers {
      post {
        request.body.then { 1 }
        request.body.then { render "ok" }
      }
    }

    then:
    requestSpec { it.body.text("foo") }
    postText() == "ratpack.http.RequestBodyAlreadyReadException: the request body has already been read"
  }

  def "can take custom action on request body too large"() {
    when:
    serverConfig {
      maxContentLength 16
    }
    handlers {
      post { ctx ->
        request.getBody({ ctx.render("foo") }).then { body ->
          response.send new String(body.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.text("bar".multiply(16)) }
    with(post()) {
      statusCode == 200
      body.text == "foo"
      headers.connection == "close"
    }
  }

  def "can read large request body over same connection"() {
    given:
    def body = "a" * (4096 * 10)
    handlers {
      post {
        request.body.then {
          context.render(directChannelAccess.channel.id().asShortText())
        }
      }
    }

    when:
    HttpURLConnection connection = applicationUnderTest.address.toURL().openConnection()
    connection.setRequestMethod("POST")
    connection.doOutput = true
    connection.outputStream << body
    def channelId1 = connection.inputStream.text

    then:
    connection.getHeaderField("Connection") == null

    when:
    connection = applicationUnderTest.address.toURL().openConnection()
    connection.setRequestMethod("POST")
    connection.doOutput = true
    connection.outputStream << body
    def channelId2 = connection.inputStream.text

    then:
    connection.getHeaderField("Connection") == null

    and:
    channelId1 == channelId2
  }

  def "request body errors when client closes connection unexpectedly"() {
    when:
    def error = new BlockingVariable<Throwable>()
    handlers {
      all { ctx ->
        request.body
          .onError { error.set(it) }
          .then { response.send(it.buffer) }
      }
    }

    and:
    Socket socket = new Socket()
    socket.connect(new InetSocketAddress(address.host, address.port))
    new OutputStreamWriter(socket.outputStream, "UTF-8").with {
      write("POST / HTTP/1.1\r\n")
      write("Content-Length: 4000\r\n")
      write("\r\n")
      close()
    }
    socket.close()

    then:
    error.get() instanceof ConnectionClosedException
  }

}
