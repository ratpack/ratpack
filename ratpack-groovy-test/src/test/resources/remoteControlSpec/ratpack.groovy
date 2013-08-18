import org.ratpackframework.remote.RemoteControlModule

import static org.ratpackframework.groovy.RatpackScript.ratpack

ratpack {
  modules {
    register new RemoteControlModule()
  }
  handlers {}
}