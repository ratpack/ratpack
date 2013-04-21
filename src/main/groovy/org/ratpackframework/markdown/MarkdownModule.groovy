package org.ratpackframework.markdown

import com.google.inject.AbstractModule
import groovy.transform.*
import org.ratpackframework.markdown.templating.*
import org.ratpackframework.templating.TemplateRenderer

@CompileStatic
@TupleConstructor(includeFields = true)
class MarkdownModule extends AbstractModule {

	private final MarkdownConfig config

	@Override
	protected void configure() {
		bind(TemplateRenderer).to(MarkdownTemplateRenderer)
		bind(MarkdownConfig).toInstance(config)
	}

}
