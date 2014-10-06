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

package ratpack.handlebars

import com.github.jknack.handlebars.Options
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static Template.handlebarsTemplate
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR

class HandlebarsTemplateRenderingSpec extends RatpackGroovyDslSpec {

  @Unroll
  void 'can render a handlebars template from #scenario'() {
    given:
    launchConfig { other(otherConfig) }
    file filePath, '{{key}}'

    when:
    bindings {
      add new HandlebarsModule(templatesPath: templatesPath)
    }
    handlers {
      get {
        render handlebarsTemplate('simple', key: 'it works!')
      }
    }

    then:
    text == 'it works!'

    where:
    scenario             | templatesPath | filePath                | otherConfig
    'default path'       | null          | 'handlebars/simple.hbs' | [:]
    'path set in module' | 'custom'      | 'custom/simple.hbs'     | [:]
    'path set in config' | null          | 'fromConfig/simple.hbs' | ['handlebars.templatesPath': "fromConfig"]
  }

  @Unroll
  void 'can configure loader suffix via #scenario'() {
    given:
    launchConfig { other(otherConfig) }
    file('handlebars/simple.hbs', '{{this}}')

    when:
    bindings {
      add new HandlebarsModule(templatesSuffix: templatesSuffix)
    }
    handlers {
      get {
        render handlebarsTemplate('simple.hbs', 'it works!')
      }
    }

    then:
    text == 'it works!'

    where:
    scenario | templatesSuffix | otherConfig
    'module' | ''              | [:]
    'config' | null            | ['handlebars.templatesSuffix': '']
  }

  void 'missing templates are handled'() {
    given:
    dir('handlebars')

    bindings {
      add new HandlebarsModule()
    }
    handlers {
      get {
        render handlebarsTemplate('simple', key: 'it works!')
      }
    }

    when:
    get()

    then:
    response.statusCode == INTERNAL_SERVER_ERROR.code()
  }

  void 'helpers can be registered'() {
    given:
    file 'handlebars/helper.hbs', '{{test}}'

    when:
    bindings {
      add new HandlebarsModule()
      bind TestHelper
    }
    handlers {
      get {
        render handlebarsTemplate('helper')
      }
    }

    then:
    text == 'from helper'
  }

  void 'content types are based on file type but can be overriden'() {
    given:
    file 'handlebars/simple.hbs', '{{this}}'
    file 'handlebars/simple.json.hbs', '{{this}}'
    file 'handlebars/simple.html.hbs', '{{this}}'

    when:
    bindings {
      add new HandlebarsModule()
    }
    handlers {
      handler {
        render handlebarsTemplate(request.path, 'content types', (String) request.queryParams.type)
      }
    }

    then:
    get("simple").headers.get(CONTENT_TYPE) == "application/octet-stream"
    get("simple.json").headers.get(CONTENT_TYPE) == "application/json"
    get("simple.html").headers.get(CONTENT_TYPE) == "text/html"
    get("simple.html?type=application/octet-stream").headers.get(CONTENT_TYPE) == "application/octet-stream"
  }

  void "templates are reloadable when reloading is enabled"() {
    given:
    file 'handlebars/simple.hbs', 'A'

    when:
    bindings {
      add new HandlebarsModule(reloadable: true)
    }
    handlers {
      get {
        render handlebarsTemplate('simple')
      }
    }

    then:
    text == 'A'

    when:
    sleep 1000 // make sure last modified times are different
    file 'handlebars/simple.hbs', 'B'

    then:
    text == 'B'
  }

  void "templates are not reloadable when reloading is disabled"() {
    given:
    file 'handlebars/simple.hbs', 'A'

    when:
    bindings {
      add new HandlebarsModule(reloadable: false)
    }
    handlers {
      get {
        render handlebarsTemplate('simple')
      }
    }

    then:
    text == 'A'

    when:
    sleep 1000 // make sure last modified times are different
    file 'handlebars/simple.hbs', 'B'

    then:
    text == 'A'
  }

  void "template cache allows templates with the same filename with different paths"() {
    given:
    file 'handlebars/foo/simple.hbs', 'A'
    file 'handlebars/bar/simple.hbs', 'B'

    when:
    bindings {
      add new HandlebarsModule(reloadable: false, cacheSize: 20)
    }
    handlers {
      get('foo') {
        render handlebarsTemplate('foo/simple')
      }
      get('bar') {
        render handlebarsTemplate('bar/simple')
      }
    }

    then:
    get('foo').body.text == 'A'
    get('bar').body.text == 'B'
  }
}

class TestHelper implements NamedHelper {

  String name = 'test'

  CharSequence apply(Object context, Options options) throws IOException {
    'from helper'
  }
}
