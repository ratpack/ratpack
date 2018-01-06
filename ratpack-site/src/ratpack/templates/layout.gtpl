yieldUnescaped '''<!doctype html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
'''

head {
  meta(charset:'utf-8')
  title('Ratpack: Lean & powerful HTTP apps for the JVM')

  meta(name: 'apple-mobile-web-app-title', content: 'Ratpack')
  meta(name: 'description', content: 'Ratpack apps are lightweight, fast, composable with other tools and libraries, easy to test and enjoyable to develop.')
  meta(name: 'viewport', content: 'width=device-width, initial-scale=1')

  link(rel: 'author', href: '/assets/humans.txt')
  link(rel: 'stylesheet', href: assets['normalize/normalize.css'])
  link(rel: 'stylesheet', href: assets['ratpack.css'])
  link(rel: 'stylesheet', href: assets['fonts.css'])

  script(src: assets['modernizr/modernizr.js']) {}
  script(src: assets['prism/prism.js']) {}
}
body {
  header(id: 'page-header') {
    div(class: 'ratpack-logo') {
      a(href: '/') { h1('Ratpack') }
      p('Lean & powerful HTTP apps')
    }

    nav(class: 'social') {
      h2('Ratpack is on&hellip;')
      a(href: 'https://github.com/ratpack/', title: 'ratpack on GitHub', rel: 'external', 'GitHub')
      newLine()
      span(class: 'join', '&amp;')
      newLine()
      a(href: 'https://twitter.com/ratpackweb', title: '@ratpackweb on Twitter', rel: 'external', 'Twitter')
    }

  }

  bodyContents()

  if (!nobanner) {
    footer(id: 'page-footer') {
      div(class: 'content') {
        section(class: 'about') {
          p {
            yield 'Ratpack is free to use, '
            a(href: 'https://github.com/ratpack/ratpack', rel: 'external', 'open source')
            yield ', and licensed under the '
            a(href: 'http://www.apache.org/licenses/LICENSE-2.0.html', rel: 'external', 'Apache License, Version 2.0.')
          }
          p {
            yield 'This site is a Ratpack application running on '
            a(href: 'https://www.heroku.com/', rel: 'external', 'Heroku')
            yield '.'
          }
          p {
            a(href: 'http://www.yourkit.com/java/profiler/index.jsp', 'YourKit')
            yield ' supports Ratpack open source project with its full-featured Java Profiler.'
          }
        }
        def credits = [
          [href: 'https://bintray.com/', img: [src: 'bintray.png', alt: 'Bintray', width: 55, height: 40]],
          [href: 'https://github.com/', img: [src: 'github.png', alt: 'Octocat', width: 122, height: 40]],
          [href: 'http://www.gradle.org/', img: [src: 'gradle.png', alt: 'Gradle logo', width: 149, height: 40]],
          [href: 'http://groovy-lang.org/', img: [src: 'groovy.png', alt: 'Groovy logo', width: 80, height: 40]],
          [href: 'https://www.heroku.com/', img: [src: 'heroku.png', alt: 'Heroku logo', width: 120, height: 40]],
          [href: 'http://netty.io/', img: [src: 'netty.png', alt: 'Netty logo', width: 80, height: 40]],
          [href: 'http://www.yourkit.com/java/profiler/index.jsp', img: [src: 'yourkit.png', alt: 'YourKit logo', width: 132, height: 43]]
        ]
        section(class: 'credits') {
          credits.each { credit ->
            a(href: credit.href, rel: 'external') {
              img(src: assets["${credit.img.src}"], alt: credit.img.alt, width: credit.img.width, height: credit.img.height)
            }
          }
        }
      }
    }
  }
  script {
    yieldUnescaped '''
(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
})(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', 'UA-41264608-1', 'ratpack.io');
ga('send', 'pageview');
'''
  }

}

yieldUnescaped '</html>'
