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

import static Template.handlebarsTemplate
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR

class HandlebarsTemplateRenderingSpec extends RatpackGroovyDslSpec {

  void 'can render a handlebars template from #scenario'() {
    given:
    write filePath, '{{key}}'

    when:
    bindings {
      module new HandlebarsModule(), {
        if (configPath) {
          it.templatesPath(configPath)
        }
      }
      if (templatesPath) {
        bindInstance(HandlebarsModule.Config, new HandlebarsModule.Config().templatesPath(templatesPath))
      }
    }
    handlers {
      get {
        render handlebarsTemplate('simple', key: 'it works!')
      }
    }

    then:
    text == 'it works!'

    where:
    scenario             | templatesPath | filePath                | configPath
    'default path'       | null          | 'handlebars/simple.hbs' | null
    'path set in module' | 'custom'      | 'custom/simple.hbs'     | null
    'path set in config' | null          | 'fromConfig/simple.hbs' | "fromConfig"
  }

  void 'can configure loader suffix via #scenario'() {
    given:
    write('handlebars/simple.hbs', '{{this}}')

    when:
    bindings {
      module new HandlebarsModule(), {
        if (configSuffix != null) {
          it.templatesSuffix(configSuffix)
        }
      }
      if (templatesSuffix != null) {
        bindInstance(HandlebarsModule.Config, new HandlebarsModule.Config().templatesSuffix(templatesSuffix))
      }
    }
    handlers {
      get {
        render handlebarsTemplate('simple.hbs', 'it works!')
      }
    }

    then:
    text == 'it works!'

    where:
    scenario | templatesSuffix | configSuffix
    'module' | ''              | null
    'config' | null            | ''
  }

  void 'missing templates are handled'() {
    given:
    mkdir('handlebars')

    bindings {
      module new HandlebarsModule()
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
    write 'handlebars/helper.hbs', '{{test}}'

    when:
    bindings {
      module new HandlebarsModule()
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
    write 'handlebars/simple.hbs', '{{this}}'
    write 'handlebars/simple.json.hbs', '{{this}}'
    write 'handlebars/simple.html.hbs', '{{this}}'

    when:
    bindings {
      module new HandlebarsModule()
    }
    handlers {
      all {
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
    write 'handlebars/simple.hbs', 'A'

    when:
    bindings {
      module new HandlebarsModule(), { it.reloadable(true) }
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
    write 'handlebars/simple.hbs', 'B'

    then:
    text == 'B'
  }

  void "templates are not reloadable when reloading is disabled"() {
    given:
    write 'handlebars/simple.hbs', 'A'

    when:
    bindings {
      module new HandlebarsModule(), { it.reloadable(false) }
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
    write 'handlebars/simple.hbs', 'B'

    then:
    text == 'A'
  }

  void "template cache allows templates with the same filename with different paths"() {
    given:
    write 'handlebars/foo/simple.hbs', 'A'
    write 'handlebars/bar/simple.hbs', 'B'

    when:
    bindings {
      module new HandlebarsModule(), { it.reloadable(false).cacheSize(20) }
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

  void 'can configure delimiters'() {
    given:
    write('handlebars/simple.hbs', '<%key%>')

    when:
    bindings {
      module new HandlebarsModule(), { it.delimiters('<%', '%>') }
    }
    handlers {
      get {
        render handlebarsTemplate('simple', key: 'it works!')
      }
    }

    then:
    text == 'it works!'
  }
}

class TestHelper implements NamedHelper {

  String name = 'test'

  CharSequence apply(Object context, Options options) throws IOException {
    'from helper'
  }
}
