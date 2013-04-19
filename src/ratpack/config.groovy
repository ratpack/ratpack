import org.ratpackframework.groovy.config.Config

(this as Config).with {

	routing.with {
		reloadable = true
		routing.staticallyCompile = true
	}

}