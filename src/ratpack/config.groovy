import org.ratpackframework.groovy.config.Config
import org.ratpackframework.markdown.MarkdownModule
import org.ratpackframework.markdown.templating.MarkdownConfig


(this as Config).with {

	modules << new MarkdownModule(new MarkdownConfig(directory: templating.directory))

	routing.with {
		reloadable = true
		routing.staticallyCompile = true
	}

}