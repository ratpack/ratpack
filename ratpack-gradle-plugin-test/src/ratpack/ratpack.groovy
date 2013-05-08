import static org.ratpackframework.groovy.RatpackScript.ratpack

ratpack {
  routing {
    get("") {
      response.send "foo"
    }
  }
}

