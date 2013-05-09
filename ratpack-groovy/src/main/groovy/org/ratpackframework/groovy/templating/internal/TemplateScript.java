package org.ratpackframework.groovy.templating.internal;

import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import org.ratpackframework.groovy.templating.Template;
import org.ratpackframework.groovy.templating.TemplateModel;
import org.ratpackframework.util.IoUtils;

import java.util.Collections;
import java.util.Map;

public abstract class TemplateScript extends Script implements Template {

  private final TemplateModel model;
  private final NestedRenderer renderer;
  private final ByteBuf buffer;

  protected TemplateScript(TemplateModel model, ByteBuf buffer, NestedRenderer renderer) {
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
