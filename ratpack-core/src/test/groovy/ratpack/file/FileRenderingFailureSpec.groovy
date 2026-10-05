/*
 * Copyright 2026 the original author or authors.
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

package ratpack.file

import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FileRenderingFailureSpec extends RatpackGroovyDslSpec {

  def errors = new ConcurrentLinkedQueue<Throwable>()
  def requestCompleted = new CountDownLatch(1)

  def "failure to open chunked file after response headers are sent closes the connection without invoking the error handler"() {
    given:
    def zip = temporaryFolder.newFolder("zip").toPath().resolve("assets.zip")
    FileSystems.newFileSystem(URI.create("jar:${zip.toUri()}"), [create: "true"]).withCloseable {
      Files.write(it.getPath("/asset.txt"), "asset".bytes)
    }
    FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:${zip.toUri()}"), [:])

    when:
    recordErrors()
    handlers {
      get {
        onClose { requestCompleted.countDown() }
        // Close the file system once the response is committed, before the body writer opens the file,
        // as happens when an application closes a zip file system during shutdown.
        response.beforeSend { zipFs.close() }
        render zipFs.getPath("/asset.txt")
      }
    }

    then:
    connectionTerminatedAfterHeaders()

    cleanup:
    zipFs?.close()
  }

  def "failure to open zero-copy file after response headers are sent closes the connection without invoking the error handler"() {
    given:
    def file = write("asset.txt", "asset")

    when:
    recordErrors()
    handlers {
      get {
        onClose { requestCompleted.countDown() }
        response.noCompress()
        response.beforeSend { Files.delete(file) }
        render file
      }
    }

    then:
    connectionTerminatedAfterHeaders()
  }

  private void recordErrors() {
    bindings {
      bindInstance ServerErrorHandler, new ServerErrorHandler() {
        @Override
        void error(Context context, Throwable throwable) throws Exception {
          errors << throwable
          context.response.status(500).send()
        }
      }
    }
  }

  private boolean connectionTerminatedAfterHeaders() {
    def port = applicationUnderTest.address.port
    def socket = new Socket("localhost", port)
    socket.soTimeout = 10_000
    def received = socket.withCloseable {
      def out = it.outputStream
      out.write("GET / HTTP/1.1\r\nHost: localhost:$port\r\n\r\n".bytes)
      out.flush()
      // Reads to EOF: the server must terminate the connection, as the response cannot be completed.
      new String(it.inputStream.bytes, "UTF-8")
    }

    assert received.startsWith("HTTP/1.1 200 OK")
    assert !received.contains("asset")
    assert requestCompleted.await(10, TimeUnit.SECONDS)
    assert errors.empty
    true
  }

}
