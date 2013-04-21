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
		Jerry html
		def handler = Stub(ResultHandler) {
			handle(_) >> { Result<ChannelBuffer> result ->
				html = $(IoUtils.utf8String(result.value))
			}
		}

		when:
		renderer.render('foo', null, handler) {
			IoUtils.utf8Buffer(markdown)
		}

		then:
		def h1 = html.find('h1')
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
