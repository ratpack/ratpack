layout 'layout.gtpl', true,
bodyContents: contents {
a(href: "http://www.reactivemanifesto.org") {
  img style: "border: 0; position: absolute; right: 0; top:0; z-index: 9000; width: 110px; height: 110px", src: "//d379ifj7s9wntv.cloudfront.net/reactivemanifesto/images/ribbons/we-are-reactive-white-right.png"
}
section(id: "promo") {
  div(class:"content") {
    article {
      h2('What is Ratpack?')

      p('Ratpack is a set of Java libraries that facilitate fast, efficient, evolvable and well tested HTTP applications.')

      p {
        yield 'It is built on the highly performant and efficient '
        a(href:"http://netty.io/", 'Netty')
        yield ' event-driven networking engine.'
      }

      p('Ratpack focuses on allowing HTTP applications to be efficient, modular, adaptive to new requirements and technologies, and well-tested over time.')
    }

    nav {
      h2('Quick Links')
      def links = [
        [link: [href: '/manual/current'], title: 'Current Manual'],
        [link: [href: '/versions'], title: 'All Versions'],
        [link: [href: 'https://github.com/ratpack/ratpack/issues', rel: 'external'], title: 'Issue Tracker'],
        [link: [href: 'http://forum.ratpack.io/'], title: 'Discussion Forum']
      ]
      ul {
        links.each { link ->
          li { a(link.link, link.title) }
        }
      }

    }
  }
}

section(id: "main") {
  article(class: "content") {
    p {
      yield 'The core of Ratpack is made up of only '
      strong("Java 8")
      yield ', '
      a(href: "http://netty.io", 'Netty')
      yield ', '
      a(href: "https://github.com/google/guava", "Google Guava")
      yield ' and '
      a(href:"https://github.com/reactive-streams/reactive-streams", "Reactive Streams")
      yield '.'
    }
    p {
      yield 'You can write Ratpack applications in Java 8 or any alternative JVM language that plays well with Java. Specific support for the '
      a(href: "http://www.groovy-lang.org", 'Groovy')
      yield ' language is provided, utilizing the latest static compilation and typing features.'
    }
    p {
      yield 'Ratpack does not take a heavily opinionated approach as to what libraries and tools you should use to compose your application. As the developer of the application, '
      strong('you')
      yield ' are in control. Direct integration of tools and libraries is favored over generic abstractions.'
    }
    p('Ratpack is for nontrivial, high performance, low resource usage, HTTP applications.')
    p {
      strong('Want to know more?')
      yield ' See some example apps @ '
      a(href: "http://github.com/ratpack", 'github.com/ratpack')
      yield ' or checkout the '
      a(href: "/manual/current", 'Ratpack Manual')
      yield '.'
    }
    p {
      strong('Have a question or problem?')
      yield ' Discuss it now via the '
      a(href: "http://slack.ratpack.io", 'Ratpack Community Channel')
      yield ' (powered by  '
      a(href: 'http://slack.com', "Slack")
      yield '). '
      a(href: "http://slack-signup.ratpack.io", "Sign up")
      yield " is required (one time)."
    }
    p {
      yield "If realtime chat isn't your thing, you can use the "
      a(href: "http://forum.ratpack.io", 'Ratpack Forum')
      yield '.'
    }
    p {
      strong('Want to contribute to Ratpack?')
      yield ' We are always eager for more contributions. Browse the '
      a(href: "http://github.com/ratpack/ratpack/issues", 'issue tracker')
      yield ' and find something to do, or raise an issue with your contribution idea.'
    }
  }
}
}
