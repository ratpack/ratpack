package ratpack.thymeleaf

import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static Template.thymeleafTemplate
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR

class ThymeleafTemplateSpec extends RatpackGroovyDslSpec {

  @Unroll
  void 'can render a thymeleaf template from #scenario'() {
    given:
    other = otherConfig
    file(filePath) << '<span th:text="${key}"/>'

    when:
    app {
      modules {
        register new ThymeleafModule(templatesPrefix: templatesPrefix)
      }
      handlers {
        get {
          render thymeleafTemplate(templateName, key: 'it works!')
        }
      }
    }

    then:
    text == '<span>it works!</span>'

    where:
    scenario                    | templatesPrefix | templateName    | filePath                       | otherConfig
    'default path'              | null            | 'simple'        | 'thymeleaf/simple.html'        | [:]
    'default nested path'       | null            | 'inside/simple' | 'thymeleaf/inside/simple.html' | [:]
    'path set in module'        | 'custom'        | 'simple'        | 'custom/simple.html'           | [:]
    'nested path set in module' | 'custom'        | 'inside/simple' | 'custom/inside/simple.html'    | [:]
    'path set in config'        | null            | 'simple'        | 'fromConfig/simple.html'       | ['thymeleaf.templatesPrefix': "fromConfig"]
  }

  @Unroll
  void 'use default suffix if a #scenario suffix is used'() {
    given:
    other = otherConfig
    file('thymeleaf/simple.html') << '<span th:text="${text}"/>'

    when:
    app {
      modules {
        register new ThymeleafModule(templatesSuffix: templatesSuffix)
      }
      handlers {
        get {
          render thymeleafTemplate('simple', text: 'it works!')
        }
      }
    }

    then:
    text == '<span>it works!</span>'

    where:
    scenario              | templatesSuffix | otherConfig
    'empty'               | ''              | [:]
    'null'                | null            | [:]
    'empty (from config)' | null            | ['thymeleaf.templatesSuffix': '']
  }

  void 'can handle abitrary suffixes'() {
    given:
    file("thymeleaf/simple${templatesSuffix}") << '<span th:text="${text}"/>'

    when:
    app {
      modules {
        register new ThymeleafModule(templatesSuffix: templatesSuffix)
      }
      handlers {
        get {
          render thymeleafTemplate('simple', text: 'it works!')
        }
      }
    }

    then:
    text == '<span>it works!</span>'

    where:
    templatesSuffix << ['.tmpl', '.thymeleaf']
  }

  void 'missing templates are handled'() {
    given:
    file('thymeleaf').mkdir()

    app {
      modules {
        register new ThymeleafModule()
      }
      handlers {
        get {
          render thymeleafTemplate('simple', key: 'it works!')
        }
      }
    }

    when:
    get()

    then:
    response.statusCode == INTERNAL_SERVER_ERROR.code()
  }

  void 'can render a thymeleaf template with variables and messages'() {
    given:
    file("thymeleaf/simple.properties") << 'greeting=Hello {0}'
    file("thymeleaf/simple.html") << '<span th:text="#{greeting(${name})}"/>'

    when:
    app {
      modules {
        register new ThymeleafModule()
      }
      handlers {
        get {
          render thymeleafTemplate('simple', name: 'John Doe')
        }
      }
    }

    then:
    text == '<span>Hello John Doe</span>'
  }

  void 'can render a thymeleaf template with fragments'() {
    given:
    file("thymeleaf/footer.html") << '<div th:fragment="copyright">page footer</div>'
    file("thymeleaf/page.html") << '<div th:include="footer :: copyright"></div>'

    when:
    app {
      modules {
        register new ThymeleafModule()
      }
      handlers {
        get {
          render thymeleafTemplate('page')
        }
      }
    }

    then:
    text == '<div>page footer</div>'
  }

  void 'surrounding html tags can be removed'() {
    given:
    file('thymeleaf/simple.html') << '<span th:text="${text}" th:remove="tag"/>'

    when:
    app {
      modules {
        register new ThymeleafModule()
      }
      handlers {
        get {
          render thymeleafTemplate('simple', text: 'it works!')
        }
      }
    }

    then:
    text == 'it works!'
  }

  @Unroll
  void 'can handle templates prefix with #scenario slash'() {
    given:
    file('thymeleaf/simple.html') << '<span th:text="${text}" th:remove="tag"/>'

    when:
    app {
      modules {
        register new ThymeleafModule(templatesPrefix: templatesPrefix)
      }
      handlers {
        get {
          render thymeleafTemplate('simple', text: 'it works!')
        }
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
    file('thymeleaf/simple.html') << '<span th:text="${text}" th:remove="tag"/>'

    when:
    app {
      modules {
        register new ThymeleafModule()
      }
      handlers {
        handler {
          render thymeleafTemplate(request.path, text: 'content types', request.queryParams.type)
        }
      }
    }

    then:
    get("simple?type=text/html").contentType == "text/html;charset=UTF-8"
    get("simple?type=text/xml").contentType == "text/xml;charset=UTF-8"
  }
}
