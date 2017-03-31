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

package ratpack.thymeleaf

import org.thymeleaf.TemplateEngine
import org.thymeleaf.cache.StandardCacheManager
import org.thymeleaf.fragment.DOMSelectorFragmentSpec
import ratpack.test.internal.RatpackGroovyDslSpec

import static Template.thymeleafTemplate
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR

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
        render thymeleafTemplate(templateName, key: 'it works!')
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
        render thymeleafTemplate('simple', key: 'it works!')
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
        render thymeleafTemplate('simple', text: 'it works!')
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
        render thymeleafTemplate('simple', text: 'it works!')
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
        render thymeleafTemplate('simple', key: 'it works!')
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
        render thymeleafTemplate('simple', name: 'John Doe')
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
        render thymeleafTemplate('simple', text: 'it works!')
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
        render thymeleafTemplate('simple', text: 'it works!')
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

  void 'content types can be set'() {
    given:
    write 'thymeleaf/simple.html', '<span th:text="${text}" th:remove="tag"/>'

    when:
    bindings {
      module new ThymeleafModule()
    }
    handlers {
      all {
        render thymeleafTemplate(request.path, text: 'content types', request.queryParams.type)
      }
    }

    then:
    get("simple?type=text/html").headers.get(CONTENT_TYPE) == "text/html"
    get("simple?type=text/xml").headers.get(CONTENT_TYPE) == "text/xml"
  }

  void 'can configure templates cache with #scenario'() {
    given:
    write 'thymeleaf/simple.html', 'DUMMY'

    when:
    TemplateEngine engine = null
    StandardCacheManager cacheManager = null

    bindings {
      module new ThymeleafModule(templatesCacheSize: templatesCacheSize)
    }

    handlers {
      all { TemplateEngine te, StandardCacheManager cm ->
        // Get the current engine
        engine = te
        cacheManager = cm

        // Call the renderer to initialize the engine
        render thymeleafTemplate("simple")
      }
    }

    then:
    // Initialize the engine
    get()

    // Be sure the engine is initialized
    engine.initialized

    // Get the resolver
    def resolver = engine.templateResolvers[0]

    // Checks
    cacheManager.templateCacheMaxSize == expectedSize
    resolver.cacheable == expectedCacheable

    where:
    scenario            | templatesCacheSize | expectedCacheable | expectedSize
    'default setttings' | null               | false             | 0
    'zero size'         | 0                  | false             | 0
    'non zero size'     | 10                 | true              | 10
  }

  void 'can register a custom dialect'() {
    given:
    write 'thymeleaf/simple.html', '<p hello:sayto="World">Hi ya!</p>'

    when:
    bindings {
      module new ThymeleafModule()
      module new HelloDialectModule()
    }
    handlers {
      all {
        render thymeleafTemplate('simple')
      }
    }

    then:
    text == '<p>Hello, World!</p>'
  }

  void 'can select a template fragment'() {
    given:
    write 'thymeleaf/with-fragment.html', '''
      <html>
        <head><title>Whatever</title></head>
        <body>Just show me this bit</body>
      </html>'''

    when:
    bindings {
      module new ThymeleafModule()
    }

    handlers {
      all {
        render thymeleafTemplate('with-fragment', new DOMSelectorFragmentSpec('body'))
      }
    }

    then:
    text == '<body>Just show me this bit</body>'

  }
}
