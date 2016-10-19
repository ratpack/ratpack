/*
 * Copyright 2016 the original author or authors.
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

package ratpack.groovy.handling

import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.test.internal.RatpackGroovyDslSpec

class GroovyOnlyIfHandlerSpec extends RatpackGroovyDslSpec {

    def "can use a closure and a handler class with when"() {
        when:
        bindings {
            bind(TestHandler)
        }
        handlers {
            onlyIf({ delegate instanceof GroovyContext }, TestHandler)
        }

        then:
        text == "from handler class"
    }

    static class TestHandler implements Handler {
        void handle(Context ctx) throws Exception {
            ctx.render "from handler class"
        }
    }
}
