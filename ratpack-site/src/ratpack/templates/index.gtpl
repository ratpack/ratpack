layout 'layout.gtpl', true,
bodyContents: contents {
a(href: "http://www.reactivemanifesto.org") {
  img style: "border: 0; position: absolute; right: 0; top:0; z-index: 9000; width: 110px; height: 110px", src: "//d379ifj7s9wntv.cloudfront.net/reactivemanifesto/images/ribbons/we-are-reactive-white-right.png"
}
section(id: "promo") {
  div(class:"content") {
    article {
      h2('What is Ratpack?')

      p "Ratpack is a set of Java libraries for building modern HTTP applications."

      p "It provides just enough for writing practical, high performance, apps."

      p {
        yield "It is built on Java 8, "
        a(href:"http://netty.io/", 'Netty')
        yield " and reactive principles."
      }
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

    a(href: "https://shop.oreilly.com/product/0636920037545.do") {
      img src: "learning-ratpack.jpg", id: "learning-ratpack", align: "right"
    }
    p {
      strong('Want to know more?')
      yield ' Check out the  '
      a(href: "/manual/current", 'manual')
      yield '.'
    }
    p {
      strong('More the bookish type?')
      yield ' Get '
      a(href: "https://shop.oreilly.com/product/0636920037545.do", '“Learning Ratpack”')
      yield ' from O\'Reilly Media.'
    }
    p {
      strong('Have a question or problem?')
      yield ' Discuss it now via the '
      a(href: "http://slack.ratpack.io", 'Ratpack Community Channel')
      yield ' (powered by  '
      a(href: 'http://slack.com', "Slack")
      yield '). '
      a(href: "http://slack-signup.ratpack.io", "One time sign up")
      yield " is required."
    }
    p {
      yield "If realtime chat isn't your thing, you can use the "
      a(href: "http://forum.ratpack.io", 'Ratpack Forum')
      yield '.'
    }
    p {
      strong('Want to contribute to Ratpack?')
      yield ' We are always eager for more contributions. Browse the '
      a(href: "https://github.com/ratpack/ratpack/issues", 'issue tracker')
      yield ' and find something to do, or raise an issue with your contribution idea.'
    }
  }
}
}
