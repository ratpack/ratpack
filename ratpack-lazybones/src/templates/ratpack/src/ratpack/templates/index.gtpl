yieldUnescaped '<!DOCTYPE html>'
html {
  head {
    meta(charset:'utf-8')
    title("Ratpack: $title")

    meta(name: 'apple-mobile-web-app-title', content: 'Ratpack')
    meta(name: 'description', content: '')
    meta(name: 'viewport', content: 'width=device-width, initial-scale=1')

    link(href: '/images/favicon.ico', rel: 'shortcut icon')
  }
  body {
    header {
      h1 'Ratpack'
      p 'Simple, lean &amp; powerful HTTP apps'
    }

    section {
      h2 title
      p 'This is the main page for your Ratpack app.'
    }

    footer {}
  }
}
