/*
 * Copyright 2018 the original author or authors.
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

package ratpack.thymeleaf3

import ratpack.test.internal.RatpackGroovyDslSpec

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import static Template.thymeleafTemplate

class ThymeleafTemplateSpec extends RatpackGroovyDslSpec {

  void 'can render a thymeleaf template from #scenario'() {
    given:
    write filePath, '<span th:text="${key}"/>'

    when:
    bindings {
      module new ThymeleafModule(templatesPrefix: templatesPrefix)
    }
    handlers {
      get {
        render thymeleafTemplate(templateName, [key: 'it works!'])
      }
    }

    then:
    text == '<span>it works!</span>'

    where:
    scenario                    | templatesPrefix | templateName    | filePath
    'default path'              | null            | 'simple'        | 'thymeleaf/simple.html'
    'default nested path'       | null            | 'inside/simple' | 'thymeleaf/inside/simple.html'
    'path set in module'        | 'custom'        | 'simple'        | 'custom/simple.html'
    'nested path set in module' | 'custom'        | 'inside/simple' | 'custom/inside/simple.html'
  }

  void 'can render a thymeleaf template from path set in config'() {
    given:
    write 'fromConfig/simple.html', '<span th:text="${key}"/>'

    when:
    bindings {
      module ThymeleafModule, { ThymeleafModule.Config config -> config.templatesPrefix("fromConfig") }
    }
    handlers {
      get {
        render thymeleafTemplate('simple', [key: 'it works!'])
      }
    }

    then:
    text == '<span>it works!</span>'
  }

  void 'use default suffix if a #scenario suffix is used'() {
    given:
    write 'thymeleaf/simple.html', '<span th:text="${text}"/>'

    when:
    bindings {
      module new ThymeleafModule(templatesSuffix: templatesSuffix), {
        if (configTemplatesSuffix != null) {
          it.templateSuffix(configTemplatesSuffix)
        }
      }
    }
    handlers {
      get {
        render thymeleafTemplate('simple', [text: 'it works!'])
      }
    }

    then:
    text == '<span>it works!</span>'

    where:
    scenario              | templatesSuffix | configTemplatesSuffix
    'empty'               | ''              | null
    'null'                | null            | null
    'empty (from config)' | null            | ''
  }

  void 'can handle abitrary suffixes'() {
    given:
    write "thymeleaf/simple${templatesSuffix}", '<span th:text="${text}"/>'

    when:
    bindings {
      module new ThymeleafModule(templatesSuffix: templatesSuffix)
    }
    handlers {
      get {
        render thymeleafTemplate('simple', [text: 'it works!'])
      }
    }

    then:
    text == '<span>it works!</span>'

    where:
    templatesSuffix << ['.tmpl', '.thymeleaf']
  }

  void 'missing templates are handled'() {
    given:
    mkdir('thymeleaf')

    bindings {
      module new ThymeleafModule()
    }
    handlers {
      get {
        render thymeleafTemplate('simple', [key: 'it works!'])
      }
    }

    when:
    get()

    then:
    response.statusCode == INTERNAL_SERVER_ERROR.code()
  }

  void 'can render a thymeleaf template with variables and messages'() {
    given:
    write "thymeleaf/simple.properties", 'greeting=Hello {0}'
    write "thymeleaf/simple.html", '<span th:text="#{greeting(${name})}"/>'

    when:
    bindings {
      module new ThymeleafModule()
    }
    handlers {
      get {
        render thymeleafTemplate('simple', [name: 'John Doe'])
      }
    }

    then:
    text == '<span>Hello John Doe</span>'
  }

  void 'can render a thymeleaf template with fragments'() {
    given:
    write "thymeleaf/footer.html", '<div th:fragment="copyright">page footer</div>'
    write "thymeleaf/page.html", '<div th:include="footer :: copyright"></div>'

    when:
    bindings {
      module new ThymeleafModule()
    }
    handlers {
      get {
        render thymeleafTemplate('page')
      }
    }

    then:
    text == '<div>page footer</div>'
  }

  void 'can render a thymeleaf template with #urlType link URL expressions'() {
    given:
    write 'thymeleaf/link.html', "<a th:href=\"${linkExpression}\">link</a>"

    when:
    bindings {
      module new ThymeleafModule()
    }
    handlers {
      get {
        render thymeleafTemplate('link')
      }
    }

    then:
    text == "<a href=\"${expectedText}\">link</a>"

    where:
    urlType             | linkExpression                       | expectedText
    'absolute'          | '@{http://example.com/absolute}'     | 'http://example.com/absolute'
    'page-relative'     | '@{page-relative/link}'              | 'page-relative/link'
    'context-relative'  | '@{/context-relative/link}'          | '/context-relative/link'
    'server-relative'   | '@{~/server-relative/link}'          | '/server-relative/link'
    'protocol-relative' | '@{//example.com/protocol-relative}' | '//example.com/protocol-relative'
  }

  void 'surrounding html tags can be removed'() {
    given:
    write 'thymeleaf/simple.html', '<span th:text="${text}" th:remove="tag"/>'

    when:
    bindings {
      module new ThymeleafModule()
    }
    handlers {
      get {
        render thymeleafTemplate('simple', [text: 'it works!'])
      }
    }

    then:
    text == 'it works!'
  }

  void 'can handle templates prefix with #scenario slash'() {
    given:
    write 'thymeleaf/simple.html', '<span th:text="${text}" th:remove="tag"/>'

    when:
    bindings {
      module new ThymeleafModule(templatesPrefix: templatesPrefix)
    }
    handlers {
      get {
        render thymeleafTemplate('simple', [text: 'it works!'])
      }
    }

    then:
    text == 'it works!'

    where:
    scenario               | templatesPrefix
    'leading and trailing' | '/thymeleaf/'
    'trailing'             | 'thymeleaf/'
    'leading'              | '/thymeleaf'
  }

}
