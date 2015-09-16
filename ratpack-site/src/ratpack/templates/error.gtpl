layout 'layout.gtpl', true,
nobanner: true,
bodyContents: contents {
  p(class: "http-status-code", style: "display: none", statusCode)
  section(id="main") {
    article(class: "content") {
      h3('Oops!')
      p(message ?: 'Something appears to have gone wrong. Sorry about that.')
    }
  }
}