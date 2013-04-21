package org.ratpackframework.markdown.templating

import java.util.concurrent.*
import com.google.inject.Inject
import groovy.transform.CompileStatic
import org.jboss.netty.buffer.ChannelBuffer
import org.pegdown.PegDownProcessor
import org.ratpackframework.handler.*
import org.ratpackframework.templating.TemplateRenderer
import org.ratpackframework.util.IoUtils
import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class MarkdownTemplateRenderer implements TemplateRenderer {

	private final File directory

	@Inject
	MarkdownTemplateRenderer(MarkdownConfig config) {
		this.directory = config.directory
	}

	@Override
	void renderTemplate(String name, Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
		render(name, model, handler) {
			IoUtils.readFile getTemplateFile(name)
		}
	}

	@Override
	@CompileStatic(SKIP)
	void renderError(Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
		handler.handle new Result<ChannelBuffer>(IoUtils.utf8Buffer("o noes $model"))
	}

	@CompileStatic(SKIP)
	private void render(String name, Map<String, ?> model, ResultHandler<ChannelBuffer> handler, Callable<? extends ChannelBuffer> bufferProvider) {
		try {
			def processor = new PegDownProcessor()
			def markdown = bufferProvider.call()
			def html = processor.markdownToHtml(IoUtils.utf8String(markdown))
			handler.handle new Result<ChannelBuffer>(IoUtils.utf8Buffer(html))
		} catch (ExecutionException e) {
			handler.handle new Result<ChannelBuffer>(e)
		}
	}

	private File getTemplateFile(String id) {
		return new File(directory, "${id}.md")
	}
}