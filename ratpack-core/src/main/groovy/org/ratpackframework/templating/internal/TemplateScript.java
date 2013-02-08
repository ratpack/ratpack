package org.ratpackframework.templating.internal;

import groovy.lang.Script;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ratpackframework.io.IoUtils;
import org.ratpackframework.templating.Template;
import org.ratpackframework.templating.TemplateModel;

import java.util.Collections;
import java.util.Map;

public abstract class TemplateScript extends Script implements Template {

  private final TemplateModel model;
  private final NestedRenderer renderer;
  private final ChannelBuffer buffer;

  protected TemplateScript(TemplateModel model, ChannelBuffer buffer, NestedRenderer renderer) {
    this.model = model;
    this.buffer = buffer;
    this.renderer = renderer;
  }

  @Override
  public TemplateModel getModel() {
    return model;
  }

  @Override
  public String render(String templateName) throws Exception {
    return render(Collections.<String, Object>emptyMap(), templateName);
  }

  @Override
  public String render(Map<String, ?> model, String templateName) throws Exception {
    renderer.render(templateName, model);
    return "";
  }

  public void $(CharSequence charSequence) {
    buffer.writeBytes(IoUtils.utf8Bytes(charSequence.toString()));
  }

}
