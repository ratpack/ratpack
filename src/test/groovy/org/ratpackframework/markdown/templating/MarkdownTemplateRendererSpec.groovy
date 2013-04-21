package org.ratpackframework.markdown.templating

import jodd.jerry.Jerry
import org.jboss.netty.buffer.ChannelBuffer
import org.ratpackframework.handler.*
import org.ratpackframework.util.IoUtils
import spock.lang.Specification
import static jodd.jerry.Jerry.jerry as $

class MarkdownTemplateRendererSpec extends Specification {

	MarkdownConfig config = new MarkdownConfig()
	def renderer = new MarkdownTemplateRenderer(config)

	void 'renders a markdown template to the response stream'() {
		given:
		def handler = new ParsingResultHandler()

		when:
		renderer.render('foo', null, handler) {
			IoUtils.utf8Buffer(markdown)
		}

		then:
		def h1 = handler.parsedResult.children().first()
		h1.is('h1')
		h1.text() == 'Heading'

		and:
		def p = h1.next()
		p.is('p')
		p.text() == 'Some text with a link to something.'

		and:
		def a = p.find('a')
		a.text() == 'a link'
		a.attr('href') == 'http://www.ratpack-framework.com/'

		where:
		markdown = '# Heading\n\nSome text with [a link](http://www.ratpack-framework.com/) to something.'
	}

}

class ParsingResultHandler implements ResultHandler<ChannelBuffer> {

	Result<ChannelBuffer> event

	@Override
	void handle(Result<ChannelBuffer> event) {
		this.event = event
	}

	Jerry getParsedResult() {
		$(IoUtils.utf8String(event.value))
	}

}