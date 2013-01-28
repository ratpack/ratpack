package org.ratpackframework.templating.internal;

import groovy.lang.Script;
import org.ratpackframework.templating.Template;
import org.ratpackframework.templating.TemplateModel;

import java.lang.ref.Reference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class TemplateScript extends Script implements Template {

  private final TemplateModel model;
  private final List<Object> parts;
  private final NestedRenderer renderer;

  private boolean outputMode = false;

  protected TemplateScript(TemplateModel model, List<Object> parts, NestedRenderer renderer) {
    this.model = model;
    this.parts = parts;
    this.renderer = renderer;
  }

  @Override
  public TemplateModel getModel() {
    return model;
  }

  protected void $o() { // enter output mode
    this.outputMode = true;
  }

  protected void $c() { // enter code mode
    this.outputMode = false;
  }

  @Override
  public void render(String templateName) {
    if (outputMode) {
      throw new InvalidTemplateException("The render() method can only be called from <% %> blocks (not ${} or <%= %> blocks)");
    }
    parts.add(renderer.render(templateName, Collections.<String, Object>emptyMap()));
  }

  @Override
  public void render(Map<String, ?> model, String templateName) {
    if (outputMode) {
      throw new InvalidTemplateException("The render() method can only be called from <% %> blocks (not ${} or <%= %> blocks)");
    }
    parts.add(renderer.render(templateName, model));
  }

  public void str(CharSequence charSequence) {
    parts.add(charSequence);
  }

}
