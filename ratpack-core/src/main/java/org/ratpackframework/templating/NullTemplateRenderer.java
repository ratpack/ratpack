package org.ratpackframework.templating;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ratpackframework.handler.ResultHandler;

import java.util.Map;

public class NullTemplateRenderer implements TemplateRenderer {

  @Override
  public void renderTemplate(String templateId, Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
    throw new UnsupportedOperationException("This template renderer cannot be used");
  }

  @Override
  public void renderError(Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
    throw new UnsupportedOperationException("This template renderer cannot be used");
  }

}

