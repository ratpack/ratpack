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

package org.ratpackframework.handlebars

import com.github.jknack.handlebars.Options
import org.ratpackframework.test.groovy.RatpackGroovyDslSpec
import spock.lang.Unroll

import static Template.handlebarsTemplate
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR

class HandlebarsTemplateRenderingSpec extends RatpackGroovyDslSpec {

  @Unroll
  void 'can render a handlebars template from #scenario'() {
    given:
    others = otherConfig
    file(filePath) << '{{key}}'

    when:
    app {
      modules {
        register new HandlebarsModule(templatesPath: templatesPath)
      }
      handlers {
        get {
          render handlebarsTemplate('simple', key: 'it works!')
        }
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
    others = otherConfig
    file('handlebars/simple.hbs') << '{{this}}'

    when:
    app {
      modules {
        register new HandlebarsModule(templatesSuffix: templatesSuffix)
      }
      handlers {
        get {
          render handlebarsTemplate('simple.hbs', 'it works!')
        }
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
    file('handlebars').mkdir()

    app {
      modules {
        register new HandlebarsModule()
      }
      handlers {
        get {
          render handlebarsTemplate('simple', key: 'it works!')
        }
      }
    }

    when:
    get()

    then:
    response.statusCode == SC_INTERNAL_SERVER_ERROR
  }

  void 'helpers can be registered'() {
    given:
    file('handlebars/helper.hbs') << '{{test}}'

    when:
    app {
      modules {
        register new HandlebarsModule()
        bind TestHelper
      }
      handlers {
        get {
          render handlebarsTemplate('helper')
        }
      }
    }

    then:
    text == 'from helper'
  }

  void 'content types are based on file type but can be overriden'() {
    given:
    file('handlebars/simple.hbs') << '{{this}}'
    file('handlebars/simple.json.hbs') << '{{this}}'
    file('handlebars/simple.html.hbs') << '{{this}}'

    when:
    app {
      modules {
        register new HandlebarsModule()
      }
      handlers {
        handler {
          render handlebarsTemplate(request.path, 'content types', request.queryParams.type)
        }
      }
    }

    then:
    get("simple").contentType == "application/octet-stream;charset=UTF-8"
    get("simple.json").contentType == "application/json;charset=UTF-8"
    get("simple.html").contentType == "text/html;charset=UTF-8"
    get("simple.html?type=application/octet-stream").contentType == "application/octet-stream;charset=UTF-8"
  }
}

class TestHelper implements NamedHelper {

  String name = 'test'

  CharSequence apply(Object context, Options options) throws IOException {
    'from helper'
  }
}
