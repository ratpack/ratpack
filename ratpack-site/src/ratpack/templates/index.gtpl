layout 'layout.gtpl', true,
bodyContents: contents {
section(id: "promo") {
  div(class:"content") {
    article {
      h2('What is Ratpack?')

      p "Ratpack is a set of Java libraries for building scalable HTTP applications."

      p "It is a lean and powerful foundation, not an all-encompassing framework."
    }

    nav {
      def links = [
        [link: [href: '/manual/current'], title: 'Documentation'],
        [link: [href: 'https://stackoverflow.com/questions/ask?tags=ratpack'], title: 'Ask for help'],
        [link: [href: 'https://github.com/ratpack/ratpack/issues', rel: 'external'], title: 'Issues'],
        [link: [href: '/versions'], title: 'Releases']
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
      yield "Alternatively, you can ask for help "
      a(href: "https://stackoverflow.com/questions/ask?tags=ratpack", 'Stack Overflow')
      yield '.'
    }
  }
}
}
