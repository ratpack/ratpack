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

package ratpack.handling

import ratpack.error.ClientErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.exec.ExecController
import ratpack.file.FileSystemBinding
import ratpack.file.MimeTypes
import ratpack.launch.LaunchConfig
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.handling.Handlers.chain

class HandlersSpec extends RatpackGroovyDslSpec {

  def "empty chain handler"() {
    when:
    handlers {
      chain([])
    }

    then:
    get().statusCode == 404
  }


  def "default services available"() {
    when:
    handlers {
      handler {
        get(ServerErrorHandler)
        get(ClientErrorHandler)
        get(MimeTypes)
        get(LaunchConfig)
        get(FileSystemBinding)
        response.send "ok"
      }
    }

    then:
    text == "ok"
  }

  def "can use the compute executor"() {
    when:
    handlers {
      get { ExecController execController ->
        promise { f ->
          execController.control.fork {
            f.success("ok")
          }
        } then {
          render it
        }
      }
    }

    then:
    text == "ok"
  }
}
