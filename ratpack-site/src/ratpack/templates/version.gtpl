layout 'layout.gtpl', true,
bodyContents: contents {
def version = model.version
section(id: "main") {
  article(class: "content") {
    h2("Version ${version.version} ${version.released ? "" : "(unreleased)"}")
    if (version.dueString() != "unscheduled") {
      p("${(version.released ? "Released " : "Due ")} on ${version.dueString()}.")
    }
    if (version.description) {
      p(version.descriptionHtml)
    }
    h3(id: "links", 'Links')
    ul {
      li {
        a(href: "/manual/$version.version/", 'Manual')
        yield ' ('
        a(href: version.manualDownloadUrl, 'zip')
        yield ')'
      }
      li { a(href: "/manual/$version.version/api/", 'API Reference') }
      li { a(href: "https://github.com/ratpack/ratpack/tree/${version.released ? "v$version.version" : "master"}", 'Source Code') }
    }
    def issueSet = issues
    h3(id: "pull-requests", "Pull Requests (${issueSet.pullRequests.size()})")
    ul {
      issueSet.pullRequests.each { issue ->
        li {
          yield '['
          a(href: issue.url, issue.number)
          yield "] - ${issue.title} ("
          a(href: issue.authorUrl, issue.author)
          yield ')'
        }
      }
    }
    h3(id: "issues", "Resolved Issues (${issueSet.issues.size()})")
    ul {
      issueSet.issues.each { issue ->
        li {
          yield '['
          a(href: issue.url, issue.number)
          yield "] - ${issue.title}"
        }
      }
    }
  }
}
}
