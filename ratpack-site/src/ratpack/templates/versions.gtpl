layout 'layout.gtpl', true,
bodyContents: contents {
section(id: "main") {
  article(class: "content") {
    h2('Released Versions')

    def versions = model.versions
    def released = versions.released
    def unreleased = versions.unreleased

    if (released) {
      p('The following are the released versions of Ratpack:')
      ul {
        released.each { version ->
          li { a(href: "/versions/${version.version}", "${version.version} (${version.due?.format("yyyy-MM-dd") ?: "unscheduled"})") }
        }
      }
    } else {
      p('No released versions of Ratpack were found at this time.')
    }

    h2('Development Versions')
    if (unreleased) {
      p('The following are the Ratpack versions currently in development:')
      ul {
        unreleased.each { version ->
          li { a(href: "/versions/${version.version}", "${version.version} (${version.due?.format("yyyy-MM-dd") ?: "unscheduled"})") }
        }
      }
    } else {
      p('No in development versions of Ratpack were found at this time.')
    }
  }
}
}
