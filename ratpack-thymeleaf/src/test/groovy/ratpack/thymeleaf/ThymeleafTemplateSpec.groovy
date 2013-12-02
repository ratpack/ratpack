package ratpack.thymeleaf

import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static Template.thymeleafTemplate

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
    scenario                     | templatesPrefix | templateName    | filePath                       | otherConfig
    'default path'               | null            | 'simple'        | 'thymeleaf/simple.html'        | [:]
    'default nested nested path' | null            | 'inside/simple' | 'thymeleaf/inside/simple.html' | [:]
    'path set in module'         | 'custom'        | 'simple'        | 'custom/simple.html'           | [:]
    'path set in config'         | null            | 'simple'        | 'fromConfig/simple.html'       | ['thymeleaf.templatesPrefix': "fromConfig"]

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
}

