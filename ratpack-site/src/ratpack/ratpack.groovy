import static org.ratpackframework.groovy.RatpackScript.ratpack

ratpack {
  handlers {
    assets "public", "index.html"
  }
}